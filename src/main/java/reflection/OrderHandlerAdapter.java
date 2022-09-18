package reflection;

import com.ib.client.Decimal;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.controller.ApiController.IOrderHandler;

public class OrderHandlerAdapter implements IOrderHandler {
	@Override public void orderState(OrderState orderState) {
	}

	@Override public void orderStatus(OrderStatus status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
	}

	@Override public void handle(int errorCode, String errorMsg) {
	}
}
