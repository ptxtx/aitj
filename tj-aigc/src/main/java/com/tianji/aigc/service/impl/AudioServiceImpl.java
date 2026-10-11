package com.tianji.aigc.service.impl;

import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        //定义流式响应对象
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();

        SpeechPrompt speechPrompt = new SpeechPrompt(text);
        Flux<SpeechResponse> streamResponse = this.openAiAudioSpeechModel.stream(speechPrompt);
        //订阅流式输出，将大模型响应的内容，以流失的方式输出给前端
        streamResponse.subscribe(speechResponse -> {
                    try {
                        byte[] bytes = speechResponse.getResult().getOutput();
                        //发送给前端
                        emitter.send(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
                );
        return emitter;
    }

    @Override
    public String stt(MultipartFile audioFile) {
        //根据上传的音频文件获取文件资源
        Resource resource = audioFile.getResource();
        //基于文件资源，创建一个音频转文字的提示
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource);

        AudioTranscriptionResponse response = this.openAiAudioTranscriptionModel.call(prompt);
        String text = response.getResult().getOutput();
        log.info("音频转文字结果,{}",text);
        return text;
    }
}
