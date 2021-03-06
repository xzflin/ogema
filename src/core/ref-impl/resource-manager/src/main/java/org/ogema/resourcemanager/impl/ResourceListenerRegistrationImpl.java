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
package org.ogema.resourcemanager.impl;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import org.ogema.core.administration.AdminApplication;
import org.ogema.core.application.ApplicationManager;

import org.ogema.core.model.Resource;
import org.ogema.resourcetree.TreeElement;

/**
 * Represents a listener registration generated by a call to
 * {@link Resource#addResourceListener(org.ogema.core.resourcemanager.ResourceListener)}. The same
 * ResourceListenerRegistration object will be used in the {@link ElementInfo} of all affected Resources, ie. all sub
 * Resources of the Resource on which the listener was registered.
 * 
 * @author jlapp
 */
@SuppressWarnings("deprecation")
public class ResourceListenerRegistrationImpl implements ResourceListenerRegistration {

	protected final ResourceBase origin;
	protected final WeakReference<org.ogema.core.resourcemanager.ResourceListener> listener;
	protected final boolean recursive;

	public ResourceListenerRegistrationImpl(ResourceBase origin, org.ogema.core.resourcemanager.ResourceListener listener, boolean recursive) {
		this.origin = origin;
		this.listener = new WeakReference<>(listener);
		this.recursive = recursive;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ResourceListenerRegistrationImpl)) {
			return false;
		}
		ResourceListenerRegistrationImpl other = (ResourceListenerRegistrationImpl) obj;
		return other.listener.get() == listener.get() && other.origin.equals(origin);
	}

	@Override
	public int hashCode() {
		return origin.hashCode();
	}

	@Override
	public void queueResourceChangedEvent(final Resource r, boolean valueChanged) {
		Resource originPathResource = null;
		if (r.equalsLocation(origin)) {
			originPathResource = origin;
		}
		else { //resource needs to be 'translated' to be relative to origin.
			String topLevelOrigin = origin.getPath("/").split("/", 2)[0];
			if (!r.getPath("/").startsWith(topLevelOrigin)) {
				for (Resource sub : origin.getSubResources(r.getResourceType(), true)) {
					if (sub.equalsLocation(r)) {
						originPathResource = sub;
						break;
					}
				}
				if (originPathResource == null) { //event source no longer reachable
					return;
				}
			}
		}
		final Resource changedResource = originPathResource != null ? originPathResource : r;

		Callable<Void> listenerCall = new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				org.ogema.core.resourcemanager.ResourceListener l = listener.get();
				if (l != null) {
					l.resourceChanged(changedResource);
				}
				return null;
			}
		};
		origin.resMan.getApplicationManager().submitEvent(listenerCall);
	}

	@Override
	public void performRegistration() {
		final ResourceDBManager manager = origin.resMan.getDatabaseManager();
		if (recursive) {
			for (TreeElement el : manager.collectSubTreeElements(origin.getEl())) {
				ElementInfo info = manager.getElementInfo(el);
				info.addResourceListener(this);
			}
		}
		else {
			ElementInfo info = manager.getElementInfo(origin.getEl());
			info.addResourceListener(this);
		}
	}

	@Override
	public void unregister() {
		final ResourceDBManager manager = origin.resMan.getDatabaseManager();
		ElementInfo info = manager.getElementInfo(origin.getEl());
		ResourceListenerRegistration reg = info.removeResourceListener(this);
		if (reg != null && reg.isRecursive()) {
			for (TreeElement el : manager.collectSubTreeElements(origin.getEl())) {
				info = manager.getElementInfo(el);
				info.removeResourceListener(this);
			}
		}
	}

	@Override
	public Resource getResource() {
		return origin;
	}

	@Override
	public AdminApplication getApplication() {
		ApplicationManager appman = origin.resMan.getApplicationManager();
		return appman.getAdministrationManager().getAppById(appman.getAppID().getIDString());
	}

	@Override
	public org.ogema.core.resourcemanager.ResourceListener getListener() {
		return listener.get();
	}

	@Override
	public boolean isRecursive() {
		return recursive;
	}

	@Override
	public boolean isAbandoned() {
		return listener.get() == null;
	}

}
