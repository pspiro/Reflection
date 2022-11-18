/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

public class EJavaSignal {
	private final Object monitor = new Object();
	private boolean open = false;

	public void issueSignal() {
		synchronized (monitor) {
			open = true;
			monitor.notifyAll();
		}
	}

	public void waitForSignal() {
		synchronized (monitor) {
			while (!open) {
				try {
					monitor.wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			open = false;
		}
	}
}
