package com.tianji.aigc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
@Tag(name = "向量数据库管理接口")
public class EmbeddingController {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    @Operation(summary = "保存文本到向量数据库")
    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        // 构建文档
        List<Document> documents = messages.stream()
                .map(message -> Document.builder().text(message).build())
                .collect(Collectors.toList());
        // 存入向量数据库
        vectorStore.add(documents);
    }
    
    @Operation(summary = "文本转向量")
    @GetMapping
    public EmbeddingResponse embed(@RequestParam("message") String message) {
        return embeddingModel.embedForResponse(List.of(message));
    }
    
    @Operation(summary = "删除文本")
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids") List<String> ids) {
        // 删除向量数据库中的数据
        vectorStore.delete(ids);
    }
    
    @Operation(summary = "内容搜索")
    @GetMapping("/search")
    public List<Document> search(@RequestParam("message") String message) {
        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .build();
        return vectorStore.similaritySearch(request);
    }
    
    @Operation(summary = "搜索全部数据")
    @GetMapping("/search/all")
    public List<Document> searchAll() {
        SearchRequest request = SearchRequest.builder()
                .query("")
                .topK(999)
                .build();
        return this.vectorStore.similaritySearch(request);
    }

}