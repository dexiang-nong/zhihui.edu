package com.tianji.aigc.service.impl;

import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.dashscope.audio.transcription.AudioTranscriptionModel;
import com.tianji.aigc.service.AudioService;
import com.tianji.media.storage.IFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {
    
    private final TextToSpeechModel textToSpeechModel;
    
    private final AudioTranscriptionModel audioTranscriptionModel;
    
    private final IFileStorage fileStorage;

    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        log.info("开始语音合成, 文本内容：{}", text);
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(text);
        Flux<TextToSpeechResponse> responseStream = textToSpeechModel.stream(speechPrompt);
        // 订阅响应流并发送数据
        responseStream.subscribe(
                speechResponse -> {
                    try {
                        // 获取响应输出的数据，并发送到响应体中
                        byte[] audioBytes = speechResponse.getResult().getOutput();
                        emitter.send(audioBytes);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );
        return emitter;
    }
    
    /*
        AudioTranscriptionModel 需要一个可以解析为 URL 的 Resource，
        但 MultipartFile.getResource() 返回的资源无法被解析为 URL。
     */
    @Override
    public String stt(MultipartFile multipartFile) {
        log.info("开始语音识别, 文件名：{}", multipartFile.getOriginalFilename());
        
        // 1. 设置文件名
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".wav";
        String fileKey = "stt-temp/" + UUID.randomUUID() + suffix;
        
        String cosUrl = null;
        try {
            // 2. 上传音频到COS
            cosUrl = fileStorage.uploadFile(fileKey, multipartFile.getInputStream(), multipartFile.getSize());
            // 3. AudioTranscriptionModel 需要一个可以解析为 URL 的 Resource，
            UrlResource audioResource = new UrlResource(cosUrl);
            // 4. 调用语音识别
            return audioTranscriptionModel.call(audioResource);
        } catch (IOException e) {
            throw new RuntimeException("语音识别失败", e);
        } finally {
            // 6. 删除 OSS 上的临时文件
            if (cosUrl != null) {
                fileStorage.deleteFile(fileKey);
            }
        }
    }
    
}
