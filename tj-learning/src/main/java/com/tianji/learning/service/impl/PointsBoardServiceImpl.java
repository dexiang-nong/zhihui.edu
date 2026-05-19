package com.tianji.learning.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.LearningConstants;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.po.PointsBoardSeason;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.tianji.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author 張德帥
 * @since 2025-04-28
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    
    private final StringRedisTemplate redisTemplate;
    
    private final PointsBoardSeasonMapper pointsBoardSeasonMapper;
    
    private final UserClient userClient;
    
    private final PointsBoardMapper pointsBoardMapper;
    
    private final TableInfoContext tableInfoContext;
    
    /**
     * 分页查询指定赛季的积分排行榜
     */
    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1. 判断查询当前赛季还是历史赛季
        Long season = query.getSeason();
        boolean isCurrentSeason = season == null || season == 0;
        // 2. 查询个人数据
        LocalDateTime now = LocalDateTime.now();
        String yyyyMM = now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        PointsBoard myBoard = isCurrentSeason
                ? queryCurrentPointsBoard(key)
                : queryHistoryPointsBoard(season);
        // 3. 查询榜单数据
        List<PointsBoard> boardList = isCurrentSeason
                ? queryCurrentPointsBoardList(key, query.getPageNo(), query.getPageSize()) // 查询当前赛季榜单 (Redis)
                : queryHistoryPointsBoardList(season, query.getPageNo(), query.getPageSize()); // 查询历史赛季榜单 (Mysql)
        // 4. 封装VO
        PointsBoardVO vo = new PointsBoardVO();
        // 4.1. 处理我的信息
        vo.setRank(myBoard.getRank());
        vo.setPoints(myBoard.getPoints());
        if (CollUtils.isEmpty(boardList)) {
            return vo;
        }
        // 4.2. 查询榜单用户信息
        List<Long> userIds = boardList.stream()
                .map(PointsBoard::getUserId)
                .collect(Collectors.toList());
        List<UserDTO> userList = userClient.queryUserByIds(userIds);
        Map<Long, String> userMap = new HashMap<>(userList.size());
        if(CollUtils.isNotEmpty(userList)) {
            userMap = userList.stream()
                    .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 4.3. 处理榜单列表
        List<PointsBoardItemVO> boardVOList = new ArrayList<>(boardList.size());
        for (PointsBoard board : boardList) {
            PointsBoardItemVO boardVO = BeanUtil.toBean(board, PointsBoardItemVO.class);
            boardVOList.add(boardVO);
            boardVO.setName(userMap.get(board.getUserId()));
        }
        vo.setBoardList(boardVOList);
        return vo;
    }
    
    /**
     * 创建积分排行榜数据库表: points_board_{seasonId}
     */
    @Override
    public void createPointsBoardTableBySeason(Integer seasonId) {
        pointsBoardMapper.createPointsBoardTableBySeason(LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId);
    }
    
    /**
     * 查询当前赛季榜单积分数据
     */
    @Override
    public List<PointsBoard> queryCurrentPointsBoardList(String key, int pageNo, int pageSize) {
        // 1. 计算start和end
        int start = (pageNo - 1) * pageSize;
        int end = start + pageSize - 1;
        // 2. 查询积分排行榜
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end);
        if (CollUtil.isEmpty(tuples)) {
            return Collections.emptyList();
        }
        // 3. 封装List
        int rank = start + 1;
        List<PointsBoard> boardList = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard entity = new PointsBoard();
            boardList.add(entity);
            entity.setUserId(Long.valueOf(userId));
            entity.setPoints(points.intValue());
            entity.setRank(rank++);
        }
        return boardList;
    }
    
    /**
     * 查询历史赛季榜单积分数据
     */
    private List<PointsBoard> queryHistoryPointsBoardList(Long seasonId, int pageNo, int pageSize) {
        // 1. 查询对应赛季是否存在
        PointsBoardSeason pointsBoardSeason = pointsBoardSeasonMapper.selectById(seasonId);
        if (pointsBoardSeason == null) {
            return Collections.emptyList(); // 查询的赛季不存在, 返回空
        }
        // 2. 判断赛季是否已经持久化
        String yyyyMM = pointsBoardSeason.getBeginTime().format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        if (BooleanUtil.isTrue(redisTemplate.hasKey(key))) {
            // 未持久化, 查询Redis
            return queryCurrentPointsBoardList(key, pageNo, pageSize);
        }
        // 3. 已持久化, 查询Mysql
        // 3.1. 将动态表名存入ThreadLocal
        tableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId);
        // 3.2. 分页查询历史赛季榜单数据
        Page<PointsBoard> page = lambdaQuery().page(new Page<>(pageNo, pageSize));
        List<PointsBoard> list = page.getRecords();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 4. 处理数据 (将id转为rank)
        list.forEach(entity -> entity.setRank(entity.getId().intValue()));
        return list;
    }
    
    /**
     * 查询当前赛季个人积分数据 (Redis)
     */
    private PointsBoard queryCurrentPointsBoard(String key) {
        Long userId = UserContext.getUser();
        // 1. 查询本用户的排名
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId.toString());
        // 2. 查询本用户的积分
        Double points = redisTemplate.opsForZSet().score(key, userId.toString());
        // 3. 封装返回
        PointsBoard board = new PointsBoard();
        board.setRank(rank == null ? 0 : rank.intValue() + 1);
        board.setPoints(points == null ? 0 : points.intValue());
        return board;
    }
    
    /**
     * 查询历史赛季个人积分数据 (Mysql)
     */
    private PointsBoard queryHistoryPointsBoard(Long seasonId) {
        Long userId = UserContext.getUser();
        PointsBoard board = new PointsBoard();
        // 1. 查询对应赛季是否存在
        PointsBoardSeason pointsBoardSeason = pointsBoardSeasonMapper.selectById(seasonId);
        if (pointsBoardSeason == null) {
            return board; // 赛季不存在, 返回空
        }
        // 2. 判断赛季是否已经持久化
        String yyyyMM = pointsBoardSeason.getBeginTime().format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + yyyyMM;
        if (BooleanUtil.isTrue(redisTemplate.hasKey(key))) {
            // 未持久化，查询Redis
            return queryCurrentPointsBoard(key);
        }
        // 3. 已持久化, 查询Mysql
        // 3.1. 将动态表名存入ThreadLocal
        tableInfoContext.setInfo(LearningConstants.POINTS_BOARD_TABLE_PREFIX + seasonId);
        // 3.2. 查询个人积分数据
        PointsBoard entity = lambdaQuery().eq(PointsBoard::getUserId, userId).one();
        if (entity == null) {
            return board;
        }
        // 4. 处理VO (将id转为rank)
        board.setRank(entity.getId().intValue());
        board.setPoints(entity.getPoints());
        return board;
    }
    
}
