/*
 * Copyright (C) 2013 Cumulocity GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package c8y.trackeragent.operations;

import org.slf4j.Logger;

import c8y.trackeragent.ConnectionRegistry;
import c8y.trackeragent.Executor;
import c8y.trackeragent.ManagedObjectCache;
import c8y.trackeragent.TrackerDevice;
import c8y.trackeragent.TrackerPlatform;
import c8y.trackeragent.logger.PlatformLogger;

import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.operation.OperationStatus;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.OperationFilter;

/**
 * Polls the platform for pending operations, executes the operations and
 * reports back the status. Operations can only be executed on devices that are
 * currently connected to the agent. Operations for devices that are currently
 * not connected are left in the queue on the platform for retry.
 * 
 */
public class OperationDispatcher implements Runnable {
    
    private final Logger logger;
    private final TrackerDevice trackerDevice;
    private TrackerPlatform platform;

    /**
     * @param platform
     *            The connection to the platform.
     * @param agent
     *            The ID of this agent.
     * @param cfgDriver
     * @param executers
     *            A map of currently connected devices. The map is maintained by
     *            the threads communicating with the devices, hence it needs to
     *            be thread-safe.
     */
    public OperationDispatcher(TrackerPlatform platform, TrackerDevice trackerDevice) throws SDKException {
        this.platform = platform;
        this.trackerDevice = trackerDevice;
        this.logger = PlatformLogger.getLogger(trackerDevice.getImei());
        
        finishExecutingOps();
    }

    public void finish(OperationRepresentation operation) throws SDKException {
        operation.setStatus(OperationStatus.SUCCESSFUL.toString());
        platform.getDeviceControlApi().update(operation);
    }

    public void fail(OperationRepresentation operation, String text, SDKException x) throws SDKException {
        operation.setStatus(OperationStatus.FAILED.toString());
        operation.setFailureReason(text + " " + x.getMessage());
        platform.getDeviceControlApi().update(operation);
    }

    /**
     * Clean up operations that are stuck in "executing" state.
     */
    private void finishExecutingOps() throws SDKException {
        logger.debug("Cancelling hanging operations");
        for (OperationRepresentation operation : byStatus(OperationStatus.EXECUTING)) {
            operation.setStatus(OperationStatus.FAILED.toString());
            platform.getDeviceControlApi().update(operation);
        }
    }

    @Override
    public void run() {
        logger.debug("Executing queued operations");
        try {
            executePendingOps();
        } catch (Exception x) {
            logger.warn("Error while executing operations", x);
        }
    }

    private void executePendingOps() throws SDKException {
        for (OperationRepresentation operation : byStatus(OperationStatus.PENDING)) {
            GId gid = operation.getDeviceId();

            TrackerDevice device = ManagedObjectCache.instance().get(gid);
            if (device == null) {
                continue; // Device hasn't been identified yet
            }

            Executor exec = ConnectionRegistry.instance().get(device.getImei());

            if (exec != null) {
                // Device is currently connected, execute on device
                executeOperation(exec, operation);
                if (OperationStatus.FAILED.toString().equals(operation.getStatus())) {
                    // Connection error, remove device
                    ConnectionRegistry.instance().remove(device.getImei());
                }
            }
        }
    }

    private void executeOperation(Executor exec, OperationRepresentation operation) throws SDKException {
        operation.setStatus(OperationStatus.EXECUTING.toString());
        platform.getDeviceControlApi().update(operation);

        try {
            exec.execute(operation);
        } catch (Exception x) {
            String msg = "Error during communication with device " + operation.getDeviceId();
            logger.warn(msg, x);
            operation.setStatus(OperationStatus.FAILED.toString());
            operation.setFailureReason(msg + x.getMessage());
        }
        platform.getDeviceControlApi().update(operation);
    }

    private Iterable<OperationRepresentation> byStatus(OperationStatus status) throws SDKException {
        OperationFilter opsFilter = new OperationFilter().byDevice(trackerDevice.getGId().getValue()).byStatus(status);
        return platform.getDeviceControlApi().getOperationsByFilter(opsFilter).get().allPages();
        //TODO unregister if device not exists?
    }
}