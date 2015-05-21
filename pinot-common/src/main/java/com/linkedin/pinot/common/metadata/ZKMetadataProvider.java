/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.common.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.helix.AccessOption;
import org.apache.helix.ZNRecord;
import org.apache.helix.store.zk.ZkHelixPropertyStore;

import com.linkedin.pinot.common.config.AbstractTableConfig;
import com.linkedin.pinot.common.config.TableNameBuilder;
import com.linkedin.pinot.common.metadata.instance.InstanceZKMetadata;
import com.linkedin.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import com.linkedin.pinot.common.metadata.segment.RealtimeSegmentZKMetadata;
import com.linkedin.pinot.common.utils.StringUtil;


public class ZKMetadataProvider {
  private static String PROPERTYSTORE_SEGMENTS_PREFIX = "/SEGMENTS";
  private static String PROPERTYSTORE_RESOURCE_CONFIGS_PREFIX = "/CONFIGS/RESOURCE";
  private static String PROPERTYSTORE_INSTANCE_CONFIGS_PREFIX = "/CONFIGS/INSTANCE";

  public static void setRealtimeTableConfig(ZkHelixPropertyStore<ZNRecord> propertyStore, String realtimeResourceName, ZNRecord znRecord) {
    propertyStore.set(constructPropertyStorePathForResourceConfig(realtimeResourceName), znRecord, AccessOption.PERSISTENT);
  }

  public static void setOfflineTableConfig(ZkHelixPropertyStore<ZNRecord> propertyStore, String offlineTableName, ZNRecord znRecord) {
    propertyStore.set(constructPropertyStorePathForResourceConfig(offlineTableName), znRecord, AccessOption.PERSISTENT);
  }

  public static void setInstanceZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, InstanceZKMetadata instanceZKMetadata) {
    ZNRecord znRecord = instanceZKMetadata.toZNRecord();
    propertyStore.set(StringUtil.join("/", PROPERTYSTORE_INSTANCE_CONFIGS_PREFIX, instanceZKMetadata.getId()), znRecord, AccessOption.PERSISTENT);
  }

  public static InstanceZKMetadata getInstanceZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, String instanceId) {
    ZNRecord znRecord = propertyStore.get(StringUtil.join("/", PROPERTYSTORE_INSTANCE_CONFIGS_PREFIX, instanceId), null, AccessOption.PERSISTENT);
    if (znRecord == null) {
      return null;
    }
    return new InstanceZKMetadata(znRecord);
  }

  public static String constructPropertyStorePathForSegment(String resourceName, String segmentName) {
    return StringUtil.join("/", PROPERTYSTORE_SEGMENTS_PREFIX, resourceName, segmentName);
  }

  public static String constructPropertyStorePathForResource(String resourceName) {
    return StringUtil.join("/", PROPERTYSTORE_SEGMENTS_PREFIX, resourceName);
  }

  public static String constructPropertyStorePathForResourceConfig(String resourceName) {
    return StringUtil.join("/", PROPERTYSTORE_RESOURCE_CONFIGS_PREFIX, resourceName);
  }

  public static boolean isSegmentExisted(ZkHelixPropertyStore<ZNRecord> propertyStore, String resourceNameForResource, String segmentName) {
    return propertyStore.exists(constructPropertyStorePathForSegment(resourceNameForResource, segmentName), AccessOption.PERSISTENT);
  }

  public static void removeResourceSegmentsFromPropertyStore(ZkHelixPropertyStore<ZNRecord> propertyStore, String resourceName) {
    String propertyStorePath = constructPropertyStorePathForResource(resourceName);
    if (propertyStore.exists(propertyStorePath, AccessOption.PERSISTENT)) {
      propertyStore.remove(propertyStorePath, AccessOption.PERSISTENT);
    }
  }

  public static void removeResourceConfigFromPropertyStore(ZkHelixPropertyStore<ZNRecord> propertyStore, String resourceName) {
    String propertyStorePath = constructPropertyStorePathForResourceConfig(resourceName);
    if (propertyStore.exists(propertyStorePath, AccessOption.PERSISTENT)) {
      propertyStore.remove(propertyStorePath, AccessOption.PERSISTENT);
    }
  }

  public static void setOfflineSegmentZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, OfflineSegmentZKMetadata offlineSegmentZKMetadata) {
    propertyStore.set(constructPropertyStorePathForSegment(
        TableNameBuilder.OFFLINE_TABLE_NAME_BUILDER.forTable(offlineSegmentZKMetadata.getResourceName()), offlineSegmentZKMetadata.getSegmentName()),
        offlineSegmentZKMetadata.toZNRecord(), AccessOption.PERSISTENT);
  }

  public static void setRealtimeSegmentZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, RealtimeSegmentZKMetadata realtimeSegmentZKMetadata) {
    propertyStore.set(constructPropertyStorePathForSegment(
        TableNameBuilder.REALTIME_TABLE_NAME_BUILDER.forTable(realtimeSegmentZKMetadata.getResourceName()), realtimeSegmentZKMetadata.getSegmentName()),
        realtimeSegmentZKMetadata.toZNRecord(), AccessOption.PERSISTENT);
  }

  public static OfflineSegmentZKMetadata getOfflineSegmentZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, String tableName, String segmentName) {
    String offlineTableName = TableNameBuilder.OFFLINE_TABLE_NAME_BUILDER.forTable(tableName);
    return new OfflineSegmentZKMetadata(propertyStore.get(constructPropertyStorePathForSegment(offlineTableName, segmentName), null, AccessOption.PERSISTENT));
  }

  public static RealtimeSegmentZKMetadata getRealtimeSegmentZKMetadata(ZkHelixPropertyStore<ZNRecord> propertyStore, String tableName, String segmentName) {
    String realtimeTableName = TableNameBuilder.REALTIME_TABLE_NAME_BUILDER.forTable(tableName);
    return new RealtimeSegmentZKMetadata(propertyStore.get(constructPropertyStorePathForSegment(realtimeTableName, segmentName), null, AccessOption.PERSISTENT));
  }

  public static AbstractTableConfig getOfflineTableConfig(ZkHelixPropertyStore<ZNRecord> propertyStore, String tableName) {
    String offlineTableName = TableNameBuilder.OFFLINE_TABLE_NAME_BUILDER.forTable(tableName);
    ZNRecord znRecord = propertyStore.get(constructPropertyStorePathForResourceConfig(offlineTableName), null, AccessOption.PERSISTENT);
    if (znRecord == null) {
      return null;
    }
    try {
      return AbstractTableConfig.fromZnRecord(znRecord);
    } catch (Exception e) {
      return null;
    }
  }

  public static AbstractTableConfig getRealtimeTableConfig(ZkHelixPropertyStore<ZNRecord> propertyStore, String tableName) {
    String realtimeTableName = TableNameBuilder.REALTIME_TABLE_NAME_BUILDER.forTable(tableName);
    ZNRecord znRecord = propertyStore.get(constructPropertyStorePathForResourceConfig(realtimeTableName), null, AccessOption.PERSISTENT);
    if (znRecord == null) {
      return null;
    }
    try {
      return AbstractTableConfig.fromZnRecord(znRecord);
    } catch (Exception e) {
      return null;
    }
  }

  public static List<OfflineSegmentZKMetadata> getOfflineSegmentZKMetadataListForTable(ZkHelixPropertyStore<ZNRecord> propertyStore, String tableName) {
    List<OfflineSegmentZKMetadata> resultList = new ArrayList<OfflineSegmentZKMetadata>();
    if (propertyStore == null) {
      return resultList;
    }
    String offlineResourceName = TableNameBuilder.OFFLINE_TABLE_NAME_BUILDER.forTable(tableName);
    if (propertyStore.exists(constructPropertyStorePathForResource(offlineResourceName), AccessOption.PERSISTENT)) {
      List<ZNRecord> znRecordList = propertyStore.getChildren(constructPropertyStorePathForResource(offlineResourceName), null, AccessOption.PERSISTENT);
      if (znRecordList != null) {
        for (ZNRecord record : znRecordList) {
          resultList.add(new OfflineSegmentZKMetadata(record));
        }
      }
    }
    return resultList;
  }

  public static List<RealtimeSegmentZKMetadata> getRealtimeSegmentZKMetadataListForTable(ZkHelixPropertyStore<ZNRecord> propertyStore, String resourceName) {
    List<RealtimeSegmentZKMetadata> resultList = new ArrayList<RealtimeSegmentZKMetadata>();
    if (propertyStore == null) {
      return resultList;
    }
    String realtimeResourceName = TableNameBuilder.REALTIME_TABLE_NAME_BUILDER.forTable(resourceName);
    if (propertyStore.exists(constructPropertyStorePathForResource(realtimeResourceName), AccessOption.PERSISTENT)) {
      List<ZNRecord> znRecordList = propertyStore.getChildren(constructPropertyStorePathForResource(realtimeResourceName), null, AccessOption.PERSISTENT);
      if (znRecordList != null) {
        for (ZNRecord record : znRecordList) {
          resultList.add(new RealtimeSegmentZKMetadata(record));
        }
      }
    }
    return resultList;
  }

}
