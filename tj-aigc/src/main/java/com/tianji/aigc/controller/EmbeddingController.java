package com.tianji.aigc.controller;

import cn.hutool.core.collection.CollStreamUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages){
        log.info("保存数据到向量库中，消息数据：{}",messages);
        List<Document> documentList = CollStreamUtil.toList(messages, message -> Document.builder()
                .text(message)
                .build()
        );
        this.vectorStore.add(documentList);
        log.info("保存数据到向量库成功，数量：{}", messages.size());
    }

    @GetMapping
    public EmbeddingResponse embed(@RequestParam("message") String message){
        return this.embeddingModel.embedForResponse(List.of(message));
    }

    //删除向量数据库中的数据
    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids") List<String>ids){
        this.vectorStore.delete(ids);
    }

    //搜索
    @GetMapping("/search")
    public List<Document> search(@RequestParam("message") String message) {
        return this.vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(5).build());
    }

    // Local retrieval-evaluation endpoint; keep it behind the existing login interceptor.
    @GetMapping("/search/eval")
    public List<Document> searchForEvaluation(
            @RequestParam("message") String message,
            @RequestParam(defaultValue = "10") int topK,
            @RequestParam(defaultValue = "0.0") double threshold) {
        if (topK < 1 || topK > 20 || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("topK must be 1..20 and threshold must be 0..1");
        }
        return this.vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(topK)
                .similarityThreshold(threshold)
                .build());
    }

    @GetMapping("/search/all")
    public List<Document> searchAll() {
        // 搜索全部数据
        return this.vectorStore.similaritySearch(SearchRequest.builder().query("").topK(999).build());
    }


}
