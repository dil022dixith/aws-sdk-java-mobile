/**
 * Copyright 2015-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mobileconnectors.s3.transferutility;

import com.amazonaws.services.s3.AmazonS3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Performs background upload and download tasks. Uses a thread pool to manage
 * the upload and download threads and limits the concurrent running threads.
 * When there are no active tasks, TransferService will stop itself.
 */
public class TransferService {

    private static final Log LOGGER = LogFactory.getLog(TransferService.class);

    /*
     * Constants of message sent to update handler.
     */
    static final int MSG_EXEC = 100;
    static final int MSG_CHECK = 200;
    static final int MSG_DISCONNECT = 300;
    static final int MSG_CONNECT = 400;

    /*
     * Constants of intent action sent to the service.
     */
    static final String INTENT_ACTION_TRANSFER_ADD = "add_transfer";
    static final String INTENT_ACTION_TRANSFER_PAUSE = "pause_transfer";
    static final String INTENT_ACTION_TRANSFER_RESUME = "resume_transfer";
    static final String INTENT_ACTION_TRANSFER_CANCEL = "cancel_transfer";
    static final String INTENT_BUNDLE_TRANSFER_ID = "id";
    static final String INTENT_BUNDLE_TRANSFER_UTILITY_OPTIONS = "transfer_utility_options";

    /*
     * Create a list of the transfer states depicting the transfers that
     * are unfinished.
     */
    static final TransferState[] UNFINISHED_TRANSFER_STATES = new TransferState[] {
        TransferState.WAITING,
        TransferState.WAITING_FOR_NETWORK,
        TransferState.IN_PROGRESS,
        TransferState.RESUMED_WAITING
    };
    
    /*
     * registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private NetworkInfoReceiver networkInfoReceiver;
    
    /*
     * A flag indicates whether a database scan is necessary. This is true when
     * service starts and when network is disconnected.
     */
    private boolean shouldScan = true;
    
    /*
     * A flag indicates whether the service is started the first time.
     */
    private boolean isReceiverNotRegistered = true;
    
    /*
     * A time-stamp when the service is last known active. The service will stop
     * after a minute of inactivity.
     */
    private volatile long lastActiveTime;

    /**
     * The identifier that identifies the invocation of onStartCommand.
     * This is used while stopping the service.
     */
    private volatile int startId;
    
    /**
     * Reference to the transfer database utility.
     */
    private final TransferDBUtil dbUtil;
    
    /**
     * The status updater that updates the state and the
     * progress of the transfer in memory and persists to the
     * database.
     */
    TransferStatusUpdater updater;
    
    private final UpdateHandler updateHandler;

    
    /**
     * The time interval to wait before checking for the
     * unfinished transfers that are not tracked in memory
     * and start them.
     * 
     * Default is 1-minute. 
     * Scan will be skipped if -1 is passed.
     * Can be changed by passing in through the
     * {@link TransferUtilityOptions}
     */
    private long transferServiceCheckTimeInterval;

    /**
     * <ul>
     * <li>The service starts upon intents from transfer utility.</li>
     * <li>It remains alive when there are active transfers.</li>
     * <li>It also stays alive when network is disconnected and there are
     * transfers waiting.</li>
     * </ul>
     */
    public TransferService() {
        LOGGER.debug("Starting Transfer Service");
        dbUtil = new TransferDBUtil();
        updater = new TransferStatusUpdater(dbUtil);
        updateHandler = new UpdateHandler();
        networkInfoReceiver = new NetworkInfoReceiver();
    }

    /**
     * A Broadcast receiver to receive network connection change events.
     */
    static class NetworkInfoReceiver /*extends BroadcastReceiver */{
//        private final Handler handler;
//        private final ConnectivityManager connManager;
//
//        /**
//         * Constructs a NetworkInfoReceiver.
//         *
//         * @param handler a handle to send message to
//         */
//        public NetworkInfoReceiver(Context context, Handler handler) {
//            this.handler = handler;
//            connManager = (ConnectivityManager) context
//                    .getSystemService(Context.CONNECTIVITY_SERVICE);
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
//                final boolean networkConnected = isNetworkConnected();
//                LOGGER.debug("Network connected: " + networkConnected);
//                handler.sendEmptyMessage(networkConnected ? MSG_CONNECT : MSG_DISCONNECT);
//            }
//        }
//
//        /**
//         * Gets the status of network connectivity.
//         *
//         * @return true if network is connected, false otherwise.
//         */
        boolean isNetworkConnected() {
            // TODO: Add ConnectivityService
            return true;
//            final NetworkInfo info = connManager.getActiveNetworkInfo();
//            return info != null && info.isConnected();
        }
    }

    public int onStartCommand(String action, Integer id, TransferUtilityOptions options, int flags, int startId) {
        this.startId = startId;

        if (isReceiverNotRegistered) {
            try {
                LOGGER.info("registering receiver");
//                this.registerReceiver(this.networkInfoReceiver,
//                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            } catch (final IllegalArgumentException iae) {
                LOGGER.warn("Ignoring the exception trying to register the receiver for connectivity change.");
            } catch (final IllegalStateException ise) {
                LOGGER.warn("Ignoring the leak in registering the receiver.");
            } finally {
                isReceiverNotRegistered = false;
            }
        }

        if (id < 0) {
            LOGGER.error("The intent sent by the TransferUtility doesn't have the id.");
            return -1;
        }
        
        final AmazonS3 s3 = S3ClientReference.get(id);
        if (s3 == null) {
            LOGGER.error("TransferService can't get s3 client and not acting on the id.");
            return -1;
        }

        final TransferUtilityOptions tuOptions = options;
        
        TransferThreadPool.init(tuOptions.getTransferThreadPoolSize());
        transferServiceCheckTimeInterval = tuOptions.getTransferServiceCheckTimeInterval();
        LOGGER.debug("ThreadPoolSize: " + tuOptions.getTransferThreadPoolSize()
            + " transferServiceCheckTimeInterval: " + tuOptions.getTransferServiceCheckTimeInterval());

        updateHandler.handleMessage(MSG_EXEC, action, id);
        
        /*
         * The service will not restart if it's killed by system.
         */
        return -1;
    }

    public void onDestroy() {
        try {
            if (networkInfoReceiver != null) {
                LOGGER.info("unregistering receiver");
//                this.unregisterReceiver(this.networkInfoReceiver);
                isReceiverNotRegistered = true;
            }
        } catch (final IllegalArgumentException iae) {
            /*
             * Ignore on purpose, just in case the service stops before
             * onStartCommand where the receiver is registered.
             */
            LOGGER.warn("exception trying to destroy the service");
        }

        pauseAll();
        TransferThreadPool.closeThreadPool();
        S3ClientReference.clear();

        LOGGER.info("Closing the database.");
    }

    class UpdateHandler {

        public void handleMessage(int msgWhat, String intentAction, Integer id) {
            LOGGER.info("Mesage " + msgWhat + ", action: " + intentAction + ", id: " + id);
            switch (msgWhat) {
                case MSG_CHECK:
                    // remove messages of the same type
                    checkTransfers();
                    break;
                case MSG_EXEC:
                    execCommand(intentAction, id);
                    break;
                case MSG_DISCONNECT:
                    pauseAllForNetwork();
                    break;
                case MSG_CONNECT:
                    checkTransfersOnNetworkReconnect();
                    break;
                default:
                    LOGGER.error("Unknown command: " + msgWhat);
                    break;
            }
        }
    }
//
    /**
     * Checks two things: whether they are active transfers and whether a
     * database scan is necessary.
     */
    void checkTransfers() {
        // scan database for previously unfinished transfers
        if (shouldScan && networkInfoReceiver.isNetworkConnected()) {
            loadAndResumeTransfersFromDB(UNFINISHED_TRANSFER_STATES);
            shouldScan = false;
        }

        // update last active time if service is active
        if (isActive()) {
            lastActiveTime = System.currentTimeMillis();
            
            // check after transferServiceCheckTimeInterval milliseconds.
//            updateHandler.sendEmptyMessageDelayed(MSG_CHECK, transferServiceCheckTimeInterval);
        } else {
            /*
             * Stop the service when it's been idled for more than the time interval supplied
             * through {@link TransferUtilityConfiguration}. The default is 1-minute.
             */
            LOGGER.debug("Stop self");
        }
    }

    /**
     * Check for the transfers that are in WAITING_FOR_NETWORK state
     * and resume them to execution.
     */
    void checkTransfersOnNetworkReconnect() {
        if (networkInfoReceiver.isNetworkConnected()) {
            loadAndResumeTransfersFromDB(new TransferState[] {TransferState.WAITING_FOR_NETWORK});
        } else {
            LOGGER.error("Network Connect message received but not connected to network.");
        }
    }

    /**
     * Executes command received by the service.
     *
     * @param intent received intent
     */
    void execCommand(String intentAction, Integer id) {
        // update last active time
        lastActiveTime = System.currentTimeMillis();

        final String action = intentAction;
        final AmazonS3 s3 = S3ClientReference.get(id);


        if (INTENT_ACTION_TRANSFER_ADD.equals(action)) {
            if (updater.getTransfer(id) != null) {
                LOGGER.warn("Transfer has already been added: " + id);
            } else {
                /*
                 * only add transfer when network is available or else relies on
                 * the network change listener to scan the database
                 */
                final TransferRecord transfer = dbUtil.getTransferById(id);
                if (transfer != null) {
                    updater.addTransfer(transfer);
                    transfer.start(s3, dbUtil, updater, networkInfoReceiver);
                } else {
                    LOGGER.error("Can't find transfer: " + id);
                }
            }
        } else if (INTENT_ACTION_TRANSFER_PAUSE.equals(action)) {
            TransferRecord transfer = updater.getTransfer(id);
            if (transfer == null) {
                transfer = dbUtil.getTransferById(id);
            }
            if (transfer != null) {
                transfer.pause(s3, updater);
            }
        } else if (INTENT_ACTION_TRANSFER_RESUME.equals(action)) {
            TransferRecord transfer = updater.getTransfer(id);
            if (transfer == null) {
                transfer = dbUtil.getTransferById(id);
                if (transfer != null) {
                    updater.addTransfer(transfer);
                } else {
                    LOGGER.error("Can't find transfer: " + id);
                }
            }
            if (transfer != null) {
                transfer.start(s3, dbUtil, updater, networkInfoReceiver);
            }
        } else if (INTENT_ACTION_TRANSFER_CANCEL.equals(action)) {
            TransferRecord transfer = updater.getTransfer(id);
            if (transfer == null) {
                transfer = dbUtil.getTransferById(id);
            }
            if (transfer != null) {
                transfer.cancel(s3, updater);
            }
        } else {
            LOGGER.error("Unknown action: " + action);
        }
    }

    /**
     * Checks whether the service is active. If a service is inactive, it can
     * stop itself safely.
     *
     * @return true if service active, false otherwise
     */
    private boolean isActive() {
        if (shouldScan) {
            return true;
        }
        for (final TransferRecord transfer : updater.getTransfers().values()) {
            if (transfer.isRunning()) {
                return true;
            }
        }
        return System.currentTimeMillis() - lastActiveTime < transferServiceCheckTimeInterval;
    }

    /**
     * Loads transfers from database. These transfers are unfinished from
     * previous session or are new transfers waiting for network. It skips any
     * transfer that is already tracked by the status updater. Also starts
     * transfers whose states indicate running but aren't.
     *
     * The transfers would start only if the AmazonS3Client is present in the
     * S3ClientReference map. If the AmazonS3Client is not present, this would
     * skip starting the transfer.
     *
     * @param transferStates The list of the transfer states 
     */
    void loadAndResumeTransfersFromDB(final TransferState[] transferStates) {
        LOGGER.debug("Loading transfers from database...");
        int count = 0;

        // Read the transfer ids from the cursor and store in this list.
        List<Integer> transferIds = new ArrayList<Integer>();

        // Query for the unfinished transfers and store them in a list
        for (Record record : Service.getInstance().getRecords()) {
            final int id = record.getId();
            // If the transfer status updater doesn't track it, load the transfer record
            // from the database and add it to the updater to track
            if (updater.getTransfer(id) == null) {
                final TransferRecord transfer = new TransferRecord();
                transfer.updateFromDB(record);
                updater.addTransfer(transfer);
                count++;
            }
            transferIds.add(id);
        }

        // Iterate over each transfer id and resume them if it's not running.
        try {
            for (final Integer id: transferIds) {
                final AmazonS3 s3 = S3ClientReference.get(id);
                if (s3 != null) {
                    // Check if it's running. If not, start the transfer.
                    final TransferRecord transfer = updater.getTransfer(id);
                    if (transfer != null && !transfer.isRunning()) {
                        transfer.start(s3, dbUtil, updater, networkInfoReceiver);
                    }
                }
            }
        } catch (final Exception exception) {
            LOGGER.error("Error in resuming the transfers." + exception.getMessage());
        }

        LOGGER.debug(count + " transfers are loaded from database.");
    }

    /**
     * Pause all running transfers and set the state to WAITING_FOR_NETWORK.
     */
    void pauseAll() {
        for (final TransferRecord transferRecord : updater.getTransfers().values()) {
            final AmazonS3 s3 = S3ClientReference.get(transferRecord.getRecord().getId());
            if (s3 != null) {
                transferRecord.pause(s3, updater);
            }
        }
    }

    /**
     * Pause all running transfers and set the state to WAITING_FOR_NETWORK.
     */
    void pauseAllForNetwork() {
        for (final TransferRecord transferRecord : updater.getTransfers().values()) {
            final AmazonS3 s3 = S3ClientReference.get(transferRecord.getRecord().getId());
            if (s3 != null && transferRecord.pause(s3, updater)) {
                updater.updateState(transferRecord.getRecord().getId(), TransferState.WAITING_FOR_NETWORK);
            }
        }
        shouldScan = true;
    }
    
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        // only available when the application is debuggable

        writer.printf("start id: %d\n", startId);
        writer.printf("network status: %s\n", networkInfoReceiver.isNetworkConnected());
        writer.printf("lastActiveTime: %s, shouldScan: %s\n", new Date(lastActiveTime), shouldScan);
        final Map<Integer, TransferRecord> transfers = updater.getTransfers();
        writer.printf("# of active transfers: %d\n", transfers.size());
        for (final TransferRecord transfer : transfers.values()) {
            Record r = transfer.getRecord();
            writer.printf("bucket: %s, key: %s, status: %s, total size: %d, current: %d\n",
                    r.getBucketName(), r.getKey(), r.getState(), r.getBytesTotal(),
                    r.getBytesCurrent());
        }
        writer.flush();
    }
}

