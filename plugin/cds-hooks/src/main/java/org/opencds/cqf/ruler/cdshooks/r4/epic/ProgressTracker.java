package org.opencds.cqf.ruler.cdshooks.r4.epic;

import java.util.concurrent.atomic.AtomicReference;

public class ProgressTracker {
	private final AtomicReference<Stage> step = new AtomicReference<>(Stage.NOT_STARTED);

	private volatile Thread worker;// populated when the task starts

	void mark(Stage s) {
		step.set(s);
	}

	Stage last() {
		return step.get();
	}

	void setWorker() {
		this.worker = Thread.currentThread();
	}

	StackTraceElement[] stack() {
		return worker != null ? worker.getStackTrace() : new StackTraceElement[0];
	}
}
