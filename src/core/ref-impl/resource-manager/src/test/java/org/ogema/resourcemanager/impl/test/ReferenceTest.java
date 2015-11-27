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
package org.ogema.resourcemanager.impl.test;

import java.util.List;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceGraphException;
import org.ogema.exam.ResourceAssertions;
import org.ogema.model.locations.Room;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.generators.ElectricHeater;
import org.ogema.model.locations.PhysicalDimensions;
import org.ogema.model.time.CalendarEntry;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * 
 * @author jlapp
 */
@ExamReactorStrategy(PerClass.class)
public class ReferenceTest extends OsgiTestBase {

	public static final String RESNAME = "ResourceTestResource";

	@Before
	@Override
	public void doBefore() {
	}

	/*
	 * test 'transitive' references i.e. A references (B references C) looks like A=C
	 */
	@Test
	public void transitiveReferencesWork() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		// sw1.addOptionalElement("ratedSwitchingCurrent");
		// FloatResource rsc1 = sw1.ratedSwitchingCurrent();
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		// sw2.addOptionalElement("ratedSwitchingCurrent");
		// FloatResource rsc2 = sw2.ratedSwitchingCurrent();
		OnOffSwitch sw3 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw3.heatCapacity().create();

		sw3.heatCapacity().setValue(3.f);

		sw2.heatCapacity().setAsReference(sw3.heatCapacity());
		sw1.heatCapacity().setAsReference(sw2.heatCapacity());

		assertEquals(3.f, sw1.heatCapacity().getValue(), 0.f);
	}

	/*
	 * test 'transitive' references i.e. A references (B references C) looks like A=C
	 */
	@Test
	public void transitiveReferencesWork2() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.settings().setpoint().create();
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw2.settings().setpoint().create();
		OnOffSwitch sw3 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.settings().setAsReference(sw2.settings());
		assertEquals(sw1.settings().setpoint().getLocation(), sw2.settings().setpoint().getLocation());
		sw3.settings().setpoint().create();
		sw2.settings().setAsReference(sw3.settings());
		System.out.println("  A " + sw1.settings().setpoint().getLocation() + ", " + sw1.settings().getLocation());
		System.out.println("  B " + sw2.settings().setpoint().getLocation() + ", " + sw2.settings().getLocation());
		System.out.println("  C " + sw3.settings().setpoint().getLocation() + ", " + sw3.settings().getLocation());
		assert (sw1.settings().setpoint().equalsLocation(sw3.settings().setpoint())) : "Rereference forwarding failed";
		assert (sw1.settings().equalsLocation(sw3.settings())) : "Rereference forwarding failed";
		sw1.delete();
		sw2.delete();
		sw3.delete();
	}

	@Test
	public void referenceForwardingWorks() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		OnOffSwitch sw3 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.stateFeedback().create();
		sw1.stateFeedback().setValue(false);
		sw2.stateFeedback().setAsReference(sw1.stateFeedback());
		sw3.stateFeedback().create();
		sw1.stateFeedback().setAsReference(sw3.stateFeedback());
		sw3.stateFeedback().setValue(true);
		System.out.printf("  sw1 (%s) stateFeedback = %s%n", sw1.getName(), sw1.stateFeedback().getLocation());
		System.out.printf("  sw2 (%s) stateFeedback = %s%n", sw2.getName(), sw2.stateFeedback().getLocation());
		System.out.printf("  sw3 (%s) stateFeedback = %s%n", sw3.getName(), sw3.stateFeedback().getLocation());

		assertTrue(sw1.stateFeedback().equalsLocation(sw2.stateFeedback()));
		assertTrue(sw1.stateFeedback().isReference(false));
		assertTrue(sw2.stateFeedback().isReference(false));

		assertEquals("Rereference forwarding failed", sw2.stateFeedback().getValue(), sw3.stateFeedback().getValue());
		assertEquals("Rereference forwarding failed", sw2.stateFeedback().getLocation(), sw3.stateFeedback()
				.getLocation());
		sw1.delete();
		sw2.delete();
		sw3.delete();
	}

	@Test
	public void equalsResourceWorks() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.heatCapacity().create();
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw2.heatCapacity().setAsReference(sw1.heatCapacity());

		assertNotSame(sw1.heatCapacity(), sw2.heatCapacity());
		assertTrue(sw1.heatCapacity().equalsLocation(sw2.heatCapacity()));
	}

	// isReference(false)
	@Test
	public void isReferenceWorks() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.addOptionalElement("stateFeedback");
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw2.setOptionalElement("stateFeedback", sw1.stateFeedback());
		assertFalse(sw1.stateFeedback().isReference(false));
		assertTrue(sw2.stateFeedback().isReference(false));
	}

	// isReference(true)
	@Test
	public void isReferencePathWorks() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.addOptionalElement("stateFeedback");
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw2.setOptionalElement("stateFeedback", sw1.stateFeedback());
		assertFalse(sw1.stateFeedback().isReference(true));
		assertTrue(sw2.stateFeedback().isReference(true));
	}

	@Test
	public void loopsCanBeCreated() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.addOptionalElement("stateFeedback");
		sw1.stateFeedback().addDecorator("fnord", sw1);
		assertEquals(2, sw1.getSubResources(true).size());
	}

	@Test
	public void referencesCanBeReplaced() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.heatCapacity().create();
		sw1.heatCapacity().setValue(1);
		sw1.heatCapacity().addDecorator("test", FloatResource.class);

		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw2.heatCapacity().create();
		sw2.heatCapacity().setValue(2);

		sw2.addDecorator("foo", sw1.heatCapacity());
		assertEquals(1, ((FloatResource) (sw2.getSubResource("foo"))).getValue(), 0f);

		FloatResource foo = (FloatResource) sw2.getSubResource("foo");
		assertEquals(1, foo.getValue(), 0f);

		sw2.addDecorator("foo", sw2.heatCapacity());
		assertEquals(2, ((FloatResource) (sw2.getSubResource("foo"))).getValue(), 0f);

		assertTrue(sw1.heatCapacity().exists());
		assertTrue(sw1.heatCapacity().getSubResource("test").exists());

		assertEquals(2, foo.getValue(), 0f);
	}

	@Test
	public void referencesToTopLevelCanBeSafelyReplaced() {
		OnOffSwitch sw1 = resMan.createResource(newResourceName(), OnOffSwitch.class);
		OnOffSwitch sw2 = resMan.createResource(newResourceName(), OnOffSwitch.class);
		ElectricHeater heater = resMan.createResource(newResourceName(), ElectricHeater.class);

		sw1.stateControl().create();
		sw2.stateControl().create();

		heater.onOffSwitch().setAsReference(sw1);
		assertTrue(heater.onOffSwitch().stateControl().exists());

		heater.onOffSwitch().setAsReference(sw2);
		assertTrue(heater.onOffSwitch().stateControl().exists());
		assertTrue(heater.onOffSwitch().stateControl().equalsLocation(sw2.stateControl()));

		assertTrue("sw1 is still complete", sw1.stateControl().exists());
	}

	@Test
	public void referencesToTopLevelCanBeSafelyDeleted() {
		OnOffSwitch sw1 = resMan.createResource(newResourceName(), OnOffSwitch.class);
		ElectricHeater heater = resMan.createResource(newResourceName(), ElectricHeater.class);
		sw1.stateControl().create();

		heater.onOffSwitch().setAsReference(sw1);

		assertTrue(heater.onOffSwitch().stateControl().equalsLocation(sw1.stateControl()));
		heater.onOffSwitch().delete();
		assertTrue("sw1 is still complete", sw1.stateControl().exists());
	}

	/* when an application holds a resource R which is a reference and this
	   reference is replaced with a new reference, R should still be valid and point
	   to the new reference (the path remains the same) */
	@Test
	public void replacedReferencesStillValid() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		OnOffSwitch sw2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		OnOffSwitch sw3 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		sw1.addOptionalElement("physDim");
		sw2.addOptionalElement("physDim");
		sw3.setOptionalElement("physDim", sw1.physDim());
		PhysicalDimensions pdim = sw3.physDim();
		assertTrue(pdim.isReference(true));
		assertTrue(pdim.getLocation("/").startsWith(sw1.getName()));
		sw3.setOptionalElement("physDim", sw2.physDim());
		assertTrue(pdim.isReference(true));
		assertTrue(pdim.getLocation("/").startsWith(sw2.getName()));
	}

	/**
	 * Checks that subresources of a resource with a decorator-reference in its path can be accessed.
	 */
	@Test
	public void refencedDecoratorsWork() {
		final String DECORATORNAME = "decorator";

		// set up the resources.
		final OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		final FloatResource value = (FloatResource) sw1.heatCapacity().create();
		value.setValue(1);
		sw1.activate(true);
		final OnOffSwitch superSwitch = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		superSwitch.addDecorator(DECORATORNAME, sw1);

		// get sw1 via the path with decorator-reference.
		final OnOffSwitch referencedSwitch = (OnOffSwitch) superSwitch.getSubResource(DECORATORNAME);
		assertNotNull(referencedSwitch);

		final List<Resource> subresources = referencedSwitch.getSubResources(false);
		assertEquals(subresources.size(), 1);

		final Resource listEntry = subresources.get(0);
		assertNotNull(listEntry);
		final FloatResource refValue = (FloatResource) referencedSwitch.heatCapacity();
		assertNotNull(refValue);

		assert (refValue.equalsLocation(listEntry));
		assert (refValue.equalsLocation(value));
		assert (!refValue.equalsPath(value));

		value.setValue(3.f);
		assertEquals(refValue.getValue(), 3.f, 1.e-6);
		refValue.setValue(4.f);
		assertEquals(value.getValue(), 4.f, 1.e-6);

		CalendarEntry meeting1 = resMan.createResource("meeting", CalendarEntry.class);
		StringResource subject1 = (StringResource) meeting1.addOptionalElement("subject");
		CalendarEntry meeting2 = resMan.createResource("meeting2", CalendarEntry.class);
		StringResource subject2 = (StringResource) meeting2.addOptionalElement("subject");
		subject1.setValue("Subject1");
		subject2.setValue("OtherSubject");

		meeting1.activate(true);
		meeting2.activate(true);

		superSwitch.addDecorator("meeting", meeting1);
		Resource subResource = superSwitch.getSubResource("meeting");
		assertTrue(subResource.equalsLocation(meeting1));
		assertFalse(subResource.equalsLocation(meeting2));

		StringResource firstSubject = (StringResource) subResource.getSubResource("subject");
		assertEquals(subject1.getValue(), firstSubject.getValue());
		superSwitch.addDecorator("meeting", meeting2);
		subResource = superSwitch.getSubResource("meeting");
		StringResource secondSubject = (StringResource) subResource.getSubResource("subject");
		assertFalse(subResource.equalsLocation(meeting1));
		assertTrue(subResource.equalsLocation(meeting2));

		assertEquals(subject2.getValue(), secondSubject.getValue());

	}

	@Test
	public void getReferencingElementsWorks() {
		OnOffSwitch sw1 = resMan.createResource(newResourceName(), OnOffSwitch.class);
		OnOffSwitch sw2 = resMan.createResource(newResourceName(), OnOffSwitch.class);
		OnOffSwitch sw3 = resMan.createResource(newResourceName(), OnOffSwitch.class);

		sw1.stateControl().create();
		BooleanResource ctrl1 = sw1.stateControl();

		assertTrue(ctrl1.getReferencingResources(Resource.class).isEmpty());

		//add a reference (optional element)
		sw2.stateControl().setAsReference(ctrl1);
		assertEquals(1, ctrl1.getReferencingResources(null).size());
		assertEquals(sw2, ctrl1.getReferencingResources(null).get(0));

		//add another reference (optional element)
		sw3.stateControl().setAsReference(ctrl1);
		assertEquals(2, ctrl1.getReferencingResources(null).size());
		assertTrue(ctrl1.getReferencingResources(null).contains(sw3));
		assertTrue(ctrl1.getReferencingResources(null).contains(sw2));

		//check method calls on a reference
		assertEquals(2, sw2.stateControl().getReferencingResources(null).size());
		assertTrue(sw2.stateControl().getReferencingResources(null).contains(sw3));
		assertTrue(sw2.stateControl().getReferencingResources(null).contains(sw2));

		//filter by refering type
		assertEquals(2, ctrl1.getReferencingResources(null).size());
		assertEquals(2, ctrl1.getReferencingResources(OnOffSwitch.class).size());
		assertEquals(0, ctrl1.getReferencingResources(Room.class).size());

		//replace existing reference (optional element)
		//  sw3.stateControl().exists(), so we cannot create the optional element
		//  by calling sw3.stateControl().create()
		sw3.addOptionalElement("stateControl");
		//sw3.stateControl().create();
		assertEquals(1, ctrl1.getReferencingResources(null).size());
		assertFalse(ctrl1.getReferencingResources(null).contains(sw3));

		//add reference (as decorator)
		sw3.addDecorator("foo", ctrl1);
		assertEquals(2, ctrl1.getReferencingResources(null).size());
		assertTrue(ctrl1.getReferencingResources(null).contains(sw3));

		//replace existing reference (decorator)
		BooleanResource dec = sw2.addDecorator("bar", BooleanResource.class);
		sw3.addDecorator("foo", dec);
		assertTrue(dec.equalsLocation(sw3.getSubResource("foo")));
		assertEquals(1, ctrl1.getReferencingResources(null).size());
		assertFalse(ctrl1.getReferencingResources(null).contains(sw3));

		//top level resources used as references have correct referencing resources
		sw3.addDecorator("wibble", sw1);
		assertEquals(1, sw1.getReferencingResources(null).size());
		assertTrue(sw1.getReferencingResources(null).contains(sw3));

	}

	@Test
	public void referencingOwnOptionalElementWorks() {
		final OnOffSwitch swtch = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		final Resource stateControl = swtch.stateControl().create();
		assertNotNull(stateControl);
		final Resource stateFeedback = swtch.stateFeedback().setAsReference(stateControl);
		assertNotNull(stateFeedback);
		assertFalse(stateControl.equalsPath(stateFeedback));
		assertTrue(stateControl.equalsLocation(stateFeedback));
	}

	/** @see #replaceReferenceByItself()  */
	@Test(expected = ResourceGraphException.class)
	public void replacingResourceWithAReferenceToItselfCausesException() {
		final OnOffSwitch swtch = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		final OnOffSwitch swtch2 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		final Resource stateControl = swtch.stateControl().create();
		assertNotNull(stateControl);
		swtch2.stateControl().setAsReference(stateControl);
		final Resource stateControl2 = swtch.stateControl().setAsReference(swtch2.stateControl());
	}

	/** @see #replacingResourceWithAReferenceToItselfCausesException() */
	@Test
	public void replaceReferenceByItself() {
		OnOffSwitch sw1 = resMan.createResource(RESNAME + counter++, OnOffSwitch.class);
		BooleanResource reference = resMan.createResource(RESNAME + counter++, BooleanResource.class);
		sw1.stateControl().setAsReference(reference);
		sw1.stateControl().setAsReference(reference);
	}

	@Test
	public void deletingReferencesWorks() {
		Room room = resMan.createResource(RESNAME + counter++, Room.class);
		room.create().activate(false);
		StringResource name = resMan.createResource(RESNAME + counter++, StringResource.class);
		name.create().activate(false);
		room.name().setAsReference(name);
		assertTrue(room.name().exists());
		assertTrue(room.name().isActive());
		room.name().delete();
		assertTrue(name.exists());
		assertTrue(name.isActive());
		assertFalse(room.name().exists());
		assertFalse(room.name().isActive());
	}

	@Test
	public void multipleReferencesFromSameParentCanBeDeleted() {
		Room room = resMan.createResource(newResourceName(), Room.class);
		room.create().activate(false);
		StringResource name = resMan.createResource(newResourceName(), StringResource.class);
		name.create().activate(false);

		room.addDecorator("x1", name);
		room.addDecorator("x2", name);
		ResourceAssertions.assertExists(room.getSubResource("x1", StringResource.class));
		ResourceAssertions.assertExists(room.getSubResource("x2", StringResource.class));

		name.delete();

		ResourceAssertions.assertDeleted(room.getSubResource("x1", StringResource.class));
		ResourceAssertions.assertDeleted(room.getSubResource("x2", StringResource.class));

	}
}
