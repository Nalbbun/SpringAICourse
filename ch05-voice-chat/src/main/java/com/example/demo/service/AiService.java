package com.example.demo.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.AudioResponseFormat;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  private ChatClient chatClient;
  private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
  private OpenAiAudioSpeechModel openAiAudioSpeechModel;

  // ##### 생성자 #####
  public AiService(ChatClient.Builder chatClientBuilder,
      OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
      OpenAiAudioSpeechModel openAiAudioSpeechModel) {
    chatClient = chatClientBuilder.build();
    this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    this.openAiAudioSpeechModel = openAiAudioSpeechModel;
  }

  // ##### 메소드 #####
  public String stt(String fileName, byte[] bytes) {
    //Spring AI 1.0.3에서는 파일 이름이 필수로 추가되어야 함.
    Resource audioResource = new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    // 모델 옵션 설정
    OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
        .model("whisper-1")
        .language("ko") // 입력 음성 언어의 종류 설정, 출력 언어에도 영향을 미침
        .build();

    // 프롬프트 생성
    AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

    // 모델을 호출하고 응답받기
    AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
    String text = response.getResult().getOutput();

    return text;
  }

  public byte[] tts(String text) {
    // 모델 옵션 설정
    OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
        .model("gpt-4o-mini-tts")
        .voice(SpeechRequest.Voice.ALLOY)
        .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
        .speed(1.0)
        .build();

    // 프롬프트 생성
    TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);

    // 모델을 호출하고 응답받기
    TextToSpeechResponse response = openAiAudioSpeechModel.call(prompt);
    byte[] bytes = response.getResult().getOutput();

    return bytes;
  }    

  // 텍스트도 같이 출력되는 음성 대화
  public Map<String, String> chatText(String question) {
    // LLM로 요청하고, 텍스트 응답 얻기
    String textAnswer = chatClient.prompt()
        .system("50자 이내로 한국어로 답변해주세요.")
        .user(question)
        .call()
        .content();

    // TTS 모델로 요청하고 응답으로 받은 음성 데이터를 base64 문자열로 변환
    byte[] audio = tts(textAnswer);
    String base64Audio = Base64.getEncoder().encodeToString(audio);

    // 텍스트 답변과 음성 답변을 Map에 저장
    Map<String, String> response = new HashMap<>();
    response.put("text", textAnswer);
    response.put("audio", base64Audio);

    return response;
  }
  
  public Flux<byte[]> ttsFlux(String text) {
    // 모델 옵션 설정
    OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
        .model("gpt-4o-mini-tts")
        .voice(SpeechRequest.Voice.ALLOY)
        .responseFormat(AudioResponseFormat.MP3)
        .speed(1.0)
        .build();

    // 프롬프트 생성
    TextToSpeechPrompt prompt = new TextToSpeechPrompt(text, options);

    // 모델로 요청하고 응답받기
    Flux<TextToSpeechResponse> response = openAiAudioSpeechModel.stream(prompt);
    Flux<byte[]> flux = response.map(speechResponse -> speechResponse.getResult().getOutput());
    return flux;
  }   
  
  // 순수 음성 대화 구현 (방법1)
  public Flux<byte[]> chatVoiceSttLlmTts(byte[] audioBytes) {
    // STT를 이용해서 음성 질문을 텍스트 질문으로 변환
    String textQuestion = stt("speech.mp3", audioBytes);

    // 텍스트 질문으로 LLM에 요청하고, 텍스트 응답 얻기
    String textAnswer = chatClient.prompt()
        .system("50자 이내로 답변해주세요.")
        .user(textQuestion)
        .call()
        .content();

    // TTS를 이용해서 비동기 음성 데이터 얻기
    Flux<byte[]> flux = ttsFlux(textAnswer);
    return flux;
  }   

  //순수 음성 대화 구현 (방법2)
  public byte[] chatVoiceOneModel(byte[] audioBytes, String mimeType) throws Exception {
    // 음성 데이터를 Resource로 생성
    Resource resource = new ByteArrayResource(audioBytes);

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        // 빈문자열이라도 제공해야함
        .text("제공되는 음성에 맞는 자연스러운 대화로 이어주세요.")
        .media(new Media(MimeType.valueOf(mimeType), resource))
        .build();

    // 모델 옵션 설정
    ChatOptions chatOptions = OpenAiChatOptions.builder()
        .model(OpenAiApi.ChatModel.GPT_4_O_MINI_AUDIO_PREVIEW)
        .outputModalities(List.of("text", "audio"))
        .outputAudio(new AudioParameters(
            ChatCompletionRequest.AudioParameters.Voice.ALLOY,
            ChatCompletionRequest.AudioParameters.AudioResponseFormat.MP3))
        .build();

    // gpt-4o-mini-audio 모델은 스트림을 지원하지 않기 때문에 동기 방식 사용
    // 모델로 요청하고 응답 받기
    ChatResponse response = chatClient.prompt()
        .system("50자 이내로 답변해주세요.")
        .messages(userMessage)
        .options(chatOptions)
        .call()
        .chatResponse();
    
    // AI 메시지 얻기
    AssistantMessage assistantMessage = response.getResult().getOutput();
    
    // 텍스트 답변 얻기
    String textAnswer = assistantMessage.getText();
    log.info("텍스트 응답: {}", textAnswer);

    // 오디오 답변 얻기
    byte[] audioAnswer = assistantMessage.getMedia().get(0).getDataAsByteArray();

    return audioAnswer;
  }  
}
