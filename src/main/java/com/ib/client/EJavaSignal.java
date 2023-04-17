/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.client;

/** Signal can come before or after client starts waiting, and client will still receive signal.
 *  I don't see the purpose of this over just using a synchronized queue */
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
