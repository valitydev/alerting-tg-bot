package dev.vality.alerting.tg.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandStateService {
    private final Set<Long> waitingForNewThreadName = ConcurrentHashMap.newKeySet();

    public void markWaitingForNewThreadName(Long chatId) {
        waitingForNewThreadName.add(chatId);
    }

    public boolean isWaitingForNewThreadName(Long chatId) {
        return waitingForNewThreadName.contains(chatId);
    }

    public void clearWaitingForNewThreadName(Long chatId) {
        waitingForNewThreadName.remove(chatId);
    }
}
