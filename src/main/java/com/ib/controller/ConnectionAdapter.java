package com.ib.controller;

import java.util.List;

import com.ib.controller.ApiController.IConnectionHandler;

import tw.util.S;

public class ConnectionAdapter implements IConnectionHandler  {

	@Override
	public void onRecNextValidId(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accountList(List<String> list) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void message(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		S.out( errorMsg);
		if (errorCode == 502) {
			S.out( "Possible duplicate API client id running on another host");
		}
	}

	@Override
	public void show(String string) {
		// TODO Auto-generated method stub
		
	}

}
