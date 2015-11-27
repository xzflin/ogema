/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ogema.tools.resourcemanipulator.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Before;
import org.junit.Test;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceManagement;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.exam.OsgiAppTestBase;
import org.ogema.tools.resourcemanipulator.ResourceManipulator;
import org.ogema.tools.resourcemanipulator.ResourceManipulatorImpl;
import org.ogema.tools.resourcemanipulator.configurations.ManipulatorConfiguration;
import org.ogema.tools.resourcemanipulator.configurations.ScheduleSum;

/**
 * Tests for schedule sum manipulator.
 * @author Marco Postigo Perez
 */
public class ScheduleSumTest extends OsgiAppTestBase {

	private static final float EPSILON = 1E-6f;
	private static final int MAX_FOR_SEED_SELECTION = 10000;
	private static final int MAX_NMB_OF_SCHEDULES = 10;
	private static final int MAX_VALUES_FOR_EACH_SCHEDULE = 1000;

	private ResourceManagement resman;

	public ScheduleSumTest() {
		super(true);
	}

	@Before
	public void setup() {
		resman = getApplicationManager().getResourceManagement();
	}

	@Test
	public void testScheduleSum() {
		// TODO for all interpolation modes ...
		final ResourceManipulator tool = new ResourceManipulatorImpl(getApplicationManager());
		tool.start();
		
		int randomInt = new Random().nextInt(MAX_FOR_SEED_SELECTION);
		// use (probable) prime as seed
		long seed = new BigInteger(String.valueOf(randomInt)).nextProbablePrime().longValue();
		Random rdn = new Random(seed);
		
		List<Schedule> schedules = new ArrayList<>();
		int nmbOfSchedules = rdn.nextInt(MAX_NMB_OF_SCHEDULES) + 1;
		for(int i = 0; i < nmbOfSchedules; ++i) {
			AbsoluteSchedule floatSchedule = createAndActivateFloatSchedule();
			assertTrue(floatSchedule.isActive());
			schedules.add(floatSchedule);
		}
		
		AbsoluteSchedule sumSchedule = createAndActivateFloatSchedule();
		assertTrue(sumSchedule.isActive());
		SumValueListener sumValueListener = new SumValueListener();
		sumSchedule.addValueListener(sumValueListener);
		
		ScheduleSum scheduleSum = tool.createConfiguration(ScheduleSum.class);
		scheduleSum.setAddends(schedules, sumSchedule);
		scheduleSum.setDelay(5000);
		scheduleSum.commit();
		
		// fill schedules and check results ...
		List<Float> sum = new ArrayList<>();
		int nmbOfValues = rdn.nextInt(MAX_VALUES_FOR_EACH_SCHEDULE) + 1;
		System.out.println("Starting schedule sum test - nmbOfValues: " + nmbOfValues + ", seed: "
				+ seed + ", nmb of schedules: " + nmbOfSchedules);
		SummaryStatistics ss = new SummaryStatistics();
		for(int i = 0; i < nmbOfValues; ++i) {
			for(Schedule s : schedules) {
				float nextFloat = rdn.nextFloat();
				s.addValue(i, new FloatValue(nextFloat));
				ss.addValue(nextFloat);
			}
			sum.add((float) ss.getSum());
			ss.clear();
		}
		
		Schedule target = scheduleSum.getTarget();
		boolean done = false;
		do {
			try {
				boolean await = sumValueListener.latch.await(20, TimeUnit.SECONDS);
				if(target.getValues(0).size() == nmbOfValues || !await) {
					done = true;
				} else {
					sumValueListener.setLatch(new CountDownLatch(1));
				}
			} catch (InterruptedException e) {
			}
		} while(!done);
		
		List<SampledValue> values = target.getValues(0);
		for(int i = 0; i < values.size(); ++i) {
			float result = values.get(i).getValue().getFloatValue();
			Float expectedResult = sum.get(i);
			assertEquals("Sum for timestamp " + i + " is not correct! Expected: " +
					expectedResult + ", but got: " + result + ", seed: " + seed, expectedResult,
					result, EPSILON);
		}
		
		tool.stop();
		tool.deleteAllConfigurations();
		// wait a second so the system can delete the configurations ... -> maybe register listener
		// and count down?
		sleep(1000);

        List<ManipulatorConfiguration> leftoverRules = tool.getConfigurations(ManipulatorConfiguration.class);
        assertTrue("Not all ManipulatorConfigurations were deleted", leftoverRules.isEmpty());
	}

	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testInputSchedReferencesOutputSched() {
		final ResourceManipulator tool = new ResourceManipulatorImpl(getApplicationManager());
		tool.start();
		
		List<Schedule> inputs = new ArrayList<>();
		for(int i = 0; i < 3; ++i) {
			AbsoluteSchedule floatSchedule = createAndActivateFloatSchedule();
			assertTrue(floatSchedule.isActive());
			inputs.add(floatSchedule);
		}
		
		int rnd = new Random().nextInt(3);
		Schedule out = createScheduleAsReference(inputs.get(rnd));
		assertTrue(out.getLocation().equals(inputs.get(rnd).getLocation()));
		assertTrue(out.isActive());
		
		ScheduleSum scheduleSum = tool.createConfiguration(ScheduleSum.class);
		scheduleSum.setAddends(inputs, out);
		scheduleSum.setDelay(1000);
		assertFalse(scheduleSum.commit());
		
	}

	private int scheduleCounter = 0;

	private AbsoluteSchedule createAndActivateFloatSchedule() {
		String name = "testfloat" + scheduleCounter++;
		final FloatResource result = resman.createResource(name, FloatResource.class);
		result.program().create();
		result.activate(true);
		return result.program();
	}

	private AbsoluteSchedule createScheduleAsReference(Schedule ref) {
		String name = "testfloat" + scheduleCounter++;
		final FloatResource result = resman.createResource(name, FloatResource.class);
		result.program().setAsReference(ref);
		result.activate(true);
		return result.program();
	}

	private class SumValueListener implements ResourceValueListener<Schedule> {

		private CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void resourceChanged(Schedule resource) {
			latch.countDown();
		}

		private void setLatch(CountDownLatch latch) {
			this.latch = latch;
		}
	}
}
