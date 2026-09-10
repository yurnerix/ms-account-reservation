package by.yurnerix.msaccountreservation.controller;

import by.yurnerix.msaccountreservation.service.ClientReportService;
import by.yurnerix.msaccountreservation.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ClientController.class, ClientReportController.class})
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected ClientService clientService;

    @MockitoBean
    protected ClientReportService clientReportService;
}
