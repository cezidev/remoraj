package com.jkoolcloud.remora.core;

import java.util.Stack;
import java.util.logging.Logger;

public class CallStack<T> extends Stack<EntryDefinition> {
	private final Logger logger;

	public CallStack(Logger logger) {
		this.logger = logger;
	}

	@Override
	public EntryDefinition push(EntryDefinition item) {
		logger.fine("Stack push: " + (size() + 1));
		return super.push(item);
	}

	@Override
	public synchronized EntryDefinition pop() {
		logger.fine("Stack pop: " + (size() - 1));
		return super.pop();
	}
}
