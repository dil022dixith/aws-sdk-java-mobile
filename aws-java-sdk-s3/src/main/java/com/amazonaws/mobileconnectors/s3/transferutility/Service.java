package com.amazonaws.mobileconnectors.s3.transferutility;

import com.gluonhq.cloudlink.client.data.DataClient;
import com.gluonhq.cloudlink.client.data.DataClientBuilder;
import com.gluonhq.cloudlink.client.data.OperationMode;
import com.gluonhq.cloudlink.client.data.SyncFlag;
import com.gluonhq.connect.ConnectState;
import com.gluonhq.connect.GluonObservableList;
import com.gluonhq.connect.provider.DataProvider;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public class Service {

    private static final Logger LOG = Logger.getLogger(Service.class.getName());
    
    private static final String DATABASE_NAME = "AWS_LOCAL_DATABASE";
    
    private final DataClient dataClient;

    private GluonObservableList<Record> records;
    
    private static Service instance;
    
    public static Service getInstance() {
        if (instance == null) {
            instance = new Service();
        }
        return instance;
    }
    
    private Service() {
        dataClient = DataClientBuilder.create()
                .operationMode(OperationMode.LOCAL_ONLY)
                .build();
        
        retrieveRecords(DATABASE_NAME);
    }
    
    private void retrieveRecords(String databaseName) {
        records = DataProvider.retrieveList(dataClient.createListDataReader(databaseName, 
                Record.class,
                SyncFlag.LIST_WRITE_THROUGH, SyncFlag.OBJECT_WRITE_THROUGH));
        records.exceptionProperty().addListener(new ChangeListener<Throwable>() {
            @Override
            public void changed(ObservableValue<? extends Throwable> observable, Throwable oldValue, Throwable newValue) {
                LOG.log(Level.WARNING, "Exception dataClient", newValue);
            }
        });
        records.stateProperty().addListener(new ChangeListener<ConnectState>() {
            @Override
            public void changed(ObservableValue<? extends ConnectState> observable, ConnectState oldValue, ConnectState newValue) {
                LOG.log(Level.INFO, "State dataClient: " + newValue);
            }
        });
        LOG.log(Level.INFO, "Initial state dataClient: " + records.getState());
    }
    
    public ObservableList<Record> getRecords() {
        return records;
    }
    
    public Record getRecord(int id) {
        for (Record record : records) {
            if (record.getId() == id) {
                return record;
            }
        }
        return new Record();
    }
    
    public void updateRecord(Record oldRecord, Record newRecord) {
        Record r = getRecord(oldRecord.getId());
        if (r.getId() == oldRecord.getId()) {
            removeRecord(oldRecord);
            addRecord(newRecord);
            dataClient.push(records);
        }
    }
    
    public void addRecord(Record record) {
        LOG.log(Level.INFO, "Adding record " + record);
        records.add(record);
        LOG.log(Level.INFO, "Push records");
        dataClient.push(records);
    }
    
    public void removeRecord(Record record) {
        LOG.log(Level.INFO, "Removing record " + record);
        records.remove(record);
        dataClient.push(records);
    }
}
