package com.gotham.joker.listeners;

import com.gotham.joker.models.Divida;
import com.gotham.joker.repository.DividaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class JokerListener {

    private final DividaRepository dividaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @Autowired
    public JokerListener(DividaRepository dividaRepository, RabbitTemplate rabbitTemplate) {
        this.dividaRepository = dividaRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "gerar-documento-divida")
    public void handleDocumentoGerado(Map<String, Object> mensagem) {
        log.info("Gerar documento: {}", mensagem);

        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    esperarAleatorio();
                    // Enviar mensagem para assinar o documento
                    rabbitTemplate.convertAndSend("gerar-documento-divida-concluido", mensagem);
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    @RabbitListener(queues = "gerar-documento-divida-concluido")
    public void handleGerarDocumentoConcluido(Map<String, Object> mensagem) {
        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    processarGeracaoDocumento(divida, mensagem);
                    esperarAleatorio();
                    log.info("Geração de documento concluída para dívida {}", mensagem.get("idDivida"));
                    rabbitTemplate.convertAndSend("assinar-documento", mensagem);
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    @RabbitListener(queues = "assinar-documento")
    public void handleAssinarDocumento(Map<String, Object> mensagem) {
        log.info("Mensagem recebida para assinar documento: {}", mensagem);

        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    esperarAleatorio();
                    rabbitTemplate.convertAndSend("assinar-documento-concluido", mensagem);
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    @RabbitListener(queues = "assinar-documento-concluido")
    public void handleAssinaturaDocumentoConcluido(Map<String, Object> mensagem) {
        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    processarAssinaturaDocumento(divida, mensagem);
                    esperarAleatorio();
                    log.info("Assinatura de documento concluída para dívida {}", mensagem.get("idDivida"));
                    rabbitTemplate.convertAndSend("executar-script", mensagem);
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    @RabbitListener(queues = "executar-script")
    public void handleExecutarScript(Map<String, Object> mensagem) {
        log.info("Mensagem recebida para executar script: {}", mensagem);

        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    esperarAleatorio();
                    rabbitTemplate.convertAndSend("executar-script-concluido", mensagem);
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    @RabbitListener(queues = "executar-script-concluido")
    public void handleExecutarScriptConcluido(Map<String, Object> mensagem) {
        Optional<Divida> dividaExistente = buscarDivida(mensagem);
        dividaExistente.ifPresentOrElse(
                divida -> {
                    divida.setExecutouScript(true);
                    dividaRepository.save(divida);
                    log.info("Script concluido para dívida {}", mensagem.get("idDivida"));
                },
                () -> log.error("idDivida não encontrado na base {}", mensagem)
        );
    }

    private Optional<Divida> buscarDivida(Map<String, Object> mensagem) {
        return dividaRepository.findById(Long.valueOf((Integer) mensagem.get("idDivida")));
    }

    private void processarGeracaoDocumento(Divida divida, Map<String, Object> mensagem) {
        divida.setHashDocumento(UUID.randomUUID().toString());
        dividaRepository.save(divida);
    }

    private void processarAssinaturaDocumento(Divida divida, Map<String, Object> mensagem) {
        divida.setHashAssinatura(UUID.randomUUID().toString());
        dividaRepository.save(divida);
    }

    private void esperarAleatorio() {
        try {
            int tempoEspera = random.nextInt(500);
            //log.info("Aguardando {} milissegundos antes de processar a próxima mensagem.", tempoEspera);
            Thread.sleep(tempoEspera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o estado de interrupção
            log.error("Erro ao aguardar: {}", e.getMessage());
        }
    }
}
