/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.pinot.core.data.recordtransformer;

import com.linkedin.pinot.core.data.GenericRow;
import javax.annotation.Nullable;


/**
 * The record transformer which takes a {@link GenericRow} and transform it based on some custom rules.
 */
public interface RecordTransformer {

  /**
   * Transforms a record based on some custom rules.
   *
   * @param record Record to transform
   * @return Transformed record, or {@code null} if the record does not follow certain rules.
   */
  @Nullable
  GenericRow transform(GenericRow record);
}
