package com.example.api;

import com.example.api.model.ScanLog;
import com.example.api.model.Mailbox;
import com.example.api.repository.ScanLogRepository;
import com.example.api.service.ScanLogService;
import com.example.api.service.ScanLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ScanLogServiceTest {

    @Mock
    private ScanLogRepository scanLogRepository;

    private ScanLogService scanLogService;

    @BeforeEach
    void setUp() {
        scanLogService = new ScanLogServiceImpl(scanLogRepository);
    }

    @Test
    void getScanLogs_ShouldReturnPageOfScanLogs() {
        Mailbox mailbox1 = new Mailbox();
        mailbox1.setId(1L);
        Mailbox mailbox2 = new Mailbox();
        mailbox2.setId(2L);
        List<Long> mailboxIds = Arrays.asList(1L, 2L);

        Pageable pageable = PageRequest.of(0, 10);

        ScanLog log1 = new ScanLog();
        log1.setMailbox(mailbox1);
        ScanLog log2 = new ScanLog();
        log2.setMailbox(mailbox2);

        List<ScanLog> scanLogs = Arrays.asList(log1, log2);
        Page<ScanLog> expectedPage = new PageImpl<>(scanLogs, pageable, scanLogs.size());

        when(scanLogRepository.findByMailboxIdIn(mailboxIds, pageable)).thenReturn(expectedPage);

        Page<ScanLog> resultPage = scanLogService.getScanLogs(mailboxIds, pageable);

        verify(scanLogRepository).findByMailboxIdIn(mailboxIds, pageable);

        assertNotNull(resultPage);
        assertEquals(2, resultPage.getContent().size());
        assertEquals(mailbox1.getId(), resultPage.getContent().get(0).getMailbox().getId());
        assertEquals(mailbox2.getId(), resultPage.getContent().get(1).getMailbox().getId());
    }

    @Test
    void getScanLogs_WithEmptyMailboxIds_ShouldReturnEmptyPage() {
        List<Long> emptyMailboxIds = Arrays.asList();
        Pageable pageable = PageRequest.of(0, 10);

        Page<ScanLog> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        when(scanLogRepository.findByMailboxIdIn(emptyMailboxIds, pageable)).thenReturn(emptyPage);

        Page<ScanLog> resultPage = scanLogService.getScanLogs(emptyMailboxIds, pageable);

        verify(scanLogRepository).findByMailboxIdIn(emptyMailboxIds, pageable);

        assertNotNull(resultPage);
        assertTrue(resultPage.getContent().isEmpty());
        assertEquals(0, resultPage.getTotalElements());
    }

    @Test
    void getScanLogs_WithMultiplePages_ShouldReturnCorrectPage() {
        Mailbox mailbox = new Mailbox();
        mailbox.setId(1L);
        List<Long> mailboxIds = Arrays.asList(1L);
        Pageable pageable = PageRequest.of(1, 5);

        ScanLog log = new ScanLog();
        log.setMailbox(mailbox);
        List<ScanLog> scanLogs = Arrays.asList(log);

        Page<ScanLog> expectedPage = new PageImpl<>(scanLogs, pageable, 6);
        when(scanLogRepository.findByMailboxIdIn(mailboxIds, pageable)).thenReturn(expectedPage);

        Page<ScanLog> resultPage = scanLogService.getScanLogs(mailboxIds, pageable);

        verify(scanLogRepository).findByMailboxIdIn(mailboxIds, pageable);

        assertNotNull(resultPage);
        assertEquals(1, resultPage.getNumber());
        assertEquals(5, resultPage.getSize());
        assertEquals(6, resultPage.getTotalElements());
        assertEquals(2, resultPage.getTotalPages());
        assertEquals(mailbox.getId(), resultPage.getContent().get(0).getMailbox().getId());
    }
}