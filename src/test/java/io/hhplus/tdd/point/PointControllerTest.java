package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.InvalidAmountException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PointService pointService;

    UserPoint userPoint;

    @BeforeEach
    void setUp() {
        userPoint = new UserPoint(1L, 100000L, System.currentTimeMillis());
    }

    @Nested
    class 포인트_조회 {

        @Test
        void 잔고_조회시_정상적으로_UserPoint를_반환한다() throws Exception {
            given(pointService.getUserPoint(anyLong())).willReturn(userPoint);

            mockMvc.perform(get("/point/{id}", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.point").value(100000L))
                    .andExpect(jsonPath("$.updateMillis").exists());
        }

        @Test
        void 내역_조회시_내역이_있으면_정상적으로_반환한다() throws Exception {
            List<PointHistory> histories = List.of(
                    new PointHistory(1L, 1L, 10000L, TransactionType.CHARGE, System.currentTimeMillis())
            );

            given(pointService.getPointHistories(anyLong())).willReturn(histories);

            mockMvc.perform(get("/point/{id}/histories", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].userId").value(1L));
        }

        @Test
        void 내역_조회시_내역이_없으면_빈_리스트를_반환한다() throws Exception {
            given(pointService.getPointHistories(anyLong())).willReturn(List.of());

            mockMvc.perform(get("/point/{id}/histories", 1L))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    class 포인트_충전 {

        @Test
        void 금액이_0원이면_InvalidAmountException_예외로_400에러를_반환한다() throws Exception {
            doThrow(new InvalidAmountException("Amount must be > 0."))
                    .when(pointService).charge(anyLong(), eq(0L));

            mockMvc.perform(patch("/point/{id}/charge", 1L)
                            .content("0")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        void 금액이_음수이면_InvalidAmountException_예외로_400에러를_반환한다() throws Exception {
            doThrow(new InvalidAmountException("Amount must be > 0."))
                    .when(pointService).charge(anyLong(), eq(-100L));

            mockMvc.perform(patch("/point/{id}/charge", 1L)
                            .content("-100")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        void 금액이_1원이상이면_충전이_성공한다() throws Exception {
            given(pointService.charge(anyLong(), anyLong())).willReturn(userPoint);

            mockMvc.perform(patch("/point/{id}/charge", 1L)
                            .content("10000")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(100000L));
        }

        @Test
        void 트랜잭션_타입이_null이면_400_에러를_반환한다() throws Exception {
            mockMvc.perform(patch("/point/{id}/charge", 1L)
                            .content("{\"amount\": 1000, \"transactionType\": null}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"))
                    .andExpect(jsonPath("$.message").value("Invalid request value."));
        }

        @Test
        void 트랜잭션_타입이_잘못된_값일_경우_400_에러를_반환한다() throws Exception {
            mockMvc.perform(patch("/point/{id}/charge", 1L)
                            .content("{\"amount\": 1000, \"transactionType\": \"INVALID_TYPE\"}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"))
                    .andExpect(jsonPath("$.message").value("Invalid request value."));
        }
    }

    @Nested
    class 포인트_사용 {

        @Test
        void 금액이_0원이면_InvalidAmountException_예외로_400에러를_반환한다() throws Exception {
            doThrow(new InvalidAmountException("Amount must be > 0."))
                    .when(pointService).use(anyLong(), eq(0L));

            mockMvc.perform(patch("/point/{id}/use", 1L)
                            .content("0")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        void 금액이_음수이면_InvalidAmountException_예외로_400에러를_반환한다() throws Exception {
            doThrow(new InvalidAmountException("Amount must be > 0."))
                    .when(pointService).use(anyLong(), eq(-100L));

            mockMvc.perform(patch("/point/{id}/use", 1L)
                            .content("-100")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("400"));
        }

        @Test
        void 금액이_1원이상이면_정상적으로_포인트를_사용한다() throws Exception {
            given(pointService.use(anyLong(), anyLong())).willReturn(userPoint);

            mockMvc.perform(patch("/point/{id}/use", 1L)
                            .content("10000")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.point").value(100000L));
        }
    }
}
