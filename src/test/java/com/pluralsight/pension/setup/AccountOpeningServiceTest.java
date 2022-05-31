package com.pluralsight.pension.setup;

import com.pluralsight.pension.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountOpeningServiceTest {

    private AccountOpeningService testObj;
    private String FIRST_NAME = "John";

    private final BackgroundCheckService backgroundCheckServiceMock = mock(BackgroundCheckService.class);
    private final ReferenceIdsManager referenceIdsManagerMock = mock(ReferenceIdsManager.class);
    private final AccountRepository accountRepositoryMock = mock(AccountRepository.class);
    private final AccountOpeningEventPublisher accountOpeningEventPublisher = mock(AccountOpeningEventPublisher.class);

    @BeforeEach
    void setUp() {
        testObj = new AccountOpeningService(backgroundCheckServiceMock,referenceIdsManagerMock ,accountRepositoryMock, accountOpeningEventPublisher);
    }

    @Test
    public void shouldOpenAccount() throws IOException {
        when(backgroundCheckServiceMock.confirm(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(new BackgroundCheckResults("acceptable_risk", 50));
        when(referenceIdsManagerMock.obtainId(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn("someId");
        final AccountOpeningStatus accountOpeningStatus = testObj.openAccount("John", "Smith", "123XYZ9", LocalDate.of(1990, 1, 1));
        assertEquals(AccountOpeningStatus.OPENED, accountOpeningStatus);
    }

    @Test
    public void shouldNotOpenAccount_HighRiskProfile() throws IOException {
        when(backgroundCheckServiceMock.confirm(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(new BackgroundCheckResults("High", 100));
        final AccountOpeningStatus accountOpeningStatus = testObj.openAccount("John", "Smith", "123XYZ9", LocalDate.of(1990, 1, 1));
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }


    @Test
    public void shouldNotOpenAccount_NullValues() throws IOException {
        when(backgroundCheckServiceMock.confirm(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(null);
        final AccountOpeningStatus accountOpeningStatus = testObj.openAccount("John", "Smith", "123XYZ9", LocalDate.of(1990, 1, 1));
        assertEquals(AccountOpeningStatus.DECLINED, accountOpeningStatus);
    }

    @Test
    public void shouldNotOpenAccount_ThrowsException() throws IOException {
        when(backgroundCheckServiceMock.confirm(eq(FIRST_NAME), anyString(), anyString(), any(LocalDate.class)))
                .thenThrow(new IOException());
        assertThrows(IOException.class, ()-> testObj.openAccount("John", "Smith", "123XYZ9", LocalDate.of(1990, 1, 1)));
    }

    @Test
    @DisplayName("Stubbing void methods")
    public void shouldNotOpenAccount_RunTimeException() throws IOException {
        final BackgroundCheckResults backgroundCheckResults = new BackgroundCheckResults("something_acceptable", 50);
        when(backgroundCheckServiceMock.confirm(anyString(), anyString(), anyString(), any(LocalDate.class)))
                .thenReturn(backgroundCheckResults);
        when(referenceIdsManagerMock.obtainId(anyString(), anyString(), anyString(), any(LocalDate.class))).thenReturn("someId");
        doThrow(new RuntimeException()).when(accountOpeningEventPublisher).notify("someId");
        assertThrows(RuntimeException.class, ()-> testObj.openAccount("John", "Smith", "123XYZ9", LocalDate.of(1990, 1, 1)));
    }


}