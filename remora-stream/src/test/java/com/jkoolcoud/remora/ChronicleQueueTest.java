/*
 *
 * Copyright (c) 2019-2020 NasTel Technologies, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of NasTel
 * Technologies, Inc. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with NasTel
 * Technologies.
 *
 * NASTEL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. NASTEL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 */

package com.jkoolcoud.remora;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.jkoolcloud.remora.core.Entry;
import com.jkoolcloud.remora.core.EntryDefinition;
import com.jkoolcloud.remora.core.EntryDefinitionDescription;
import com.jkoolcloud.remora.core.Exit;
import com.jkoolcloud.tnt4j.streams.configure.ChronicleQueueProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.ChronicleQueueStream;
import com.jkoolcloud.tnt4j.streams.inputs.StreamThread;
import com.jkoolcloud.tnt4j.streams.outputs.NullActivityOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJavaObjectParser;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;

/**
 * @author akausinis
 * @version 1.0
 * @created 2019-10-31 11:29
 */
public class ChronicleQueueTest {
	private EntryDefinition expect;

	@Test
	public void streamTest() throws Exception {
		// System.setProperty("log4j.configuration", "file:./config/log4j.properties");
		// ChronicleQueueStream stream = new ChronicleQueueStream();
		// stream.setProperty(StreamProperties.PROP_FILENAME,
		// "c:\\Users\\slabs\\AppData\\Local\\Temp\\chronicle-queue4202994159695497585\\");
		// stream.setProperty(ChronicleQueueProperties.PROP_MARSHALL_CLASS,
		// "com.jkoolcloud.remora.core.EntryDefinition");
		// stream.addReference(new NullActivityOutput());
		// stream.startStream();
		//
		// Object nextItem = stream.getNextItem();
		// System.out.println(nextItem);
		// assertNotNull(nextItem);
	}

	@Test
	public void streamTestWithCreateQueue() throws Exception {
		System.setProperty("log4j.configuration", "file:./config/log4j.properties");
		ChronicleQueueStream stream = new ChronicleQueueStream() {
			@Override
			public Object getNextItem() throws Exception {
				Object item = super.getNextItem();
				System.out.println("####################################################################" + item);
				assertEquals(item, expect);

				return item;
			}
		};

		Path testQueue = Files.createTempDirectory("testQueue");

		ChronicleQueue queue = ChronicleQueue.single(testQueue.toFile().getAbsolutePath());
		ExcerptAppender appender = queue.acquireAppender();

		stream.setProperty(StreamProperties.PROP_FILENAME, testQueue.toFile().getAbsolutePath());
		stream.setProperty(ChronicleQueueProperties.PROP_MARSHALL_CLASS, "com.jkoolcloud.remora.core.Exit");
		stream.addReference(new NullActivityOutput());
		stream.addParser(new ActivityJavaObjectParser());

		StreamThread streamThread = new StreamThread(stream);

		streamThread.start();

		expect = new EntryDefinition(ChronicleQueueTest.class);
		expect.setName("AAA");
		appender.writeDocument(expect.entry);
		Thread.sleep(300);
		expect = new EntryDefinition(ChronicleQueueTest.class);
		expect.setDuration(400L);

		appender.writeDocument(expect.exit);

		Thread.sleep(300);

		expect = new EntryDefinition(ChronicleQueueTest.class);
		expect.setException("Exeption");
		appender.writeDocument(expect.entry);
		Thread.sleep(300);
		expect = new EntryDefinition(ChronicleQueueTest.class);
		appender.writeDocument(expect.exit);
		Thread.sleep(300);
	}

	int countEntry = 0;
	int countExit = 0;
	int countOther = 0;

	int countEntryMethod = 0;
	int countExitMethod = 0;

	@Test
	public void testReadQueue() {

		ChronicleQueue queue = ChronicleQueue
				.singleBuilder("c:\\workspace\\build\\remora\\remora-0.1.3-SNAPSHOT\\queue\\").build();

		// .singleBuilder("c:\\workspace\\build\\remora\\remora-0.1.3-SNAPSHOT\\q").build();

		ExcerptTailer tailer = queue.createTailer();
		MethodReader methodReader = tailer.methodReader(new EntryDefinitionDescription() {
			@Override
			public void entry(Entry entry) {
				System.out.println("Entry " + entry.toString());
				countEntryMethod++;
			}

			@Override
			public void exit(Exit exit) {
				System.out.println("Exit " + exit.toString());
				countExitMethod++;
			}
		});

		while (methodReader.readOne()) {
		}

		tailer.toStart();

		while (readOne0(tailer)) {

		}
		System.out.println(countEntry + " " + countExit + " " + countOther);

		assertEquals(countEntry, countEntryMethod);
		assertEquals(countExit, countEntryMethod);
		assertEquals(0, countOther);
	}

	private boolean readOne0(ExcerptTailer in) {
		try (DocumentContext context = in.readingDocument()) {
			if (!context.isPresent()) {
				return false;
			}
			if (context.isMetaData()) {
				readOneMetaData(context);
				return true;
			}
			assert context.isData();

			accept(context.wire());
		}
		return true;
	}

	private void accept(Wire wire) {

		StringBuilder sb = new StringBuilder();
		ValueIn valueIn = wire.readEventName(sb);
		if (sb.toString().equalsIgnoreCase("entry")) {
			countEntry++;
			Entry entry = Wires.object0(valueIn, new Entry(), Entry.class);
			System.out.println(entry.toString());
		} else if (sb.toString().equalsIgnoreCase("exit")) {
			countExit++;
			Exit exit = Wires.object0(valueIn, new Exit(), Exit.class);
			System.out.println(exit.toString());
		} else {
			countOther++;
		}

	}

	private boolean readOneMetaData(DocumentContext context) {
		System.out.println("META");
		StringBuilder sb = Wires.acquireStringBuilder();
		Wire wire = context.wire();
		Bytes<?> bytes = wire.bytes();
		long r = bytes.readPosition();
		wire.readEventName(sb);
		// roll back position to where is was before we read the SB
		bytes.readPosition(r);
		return true;
	}
}
