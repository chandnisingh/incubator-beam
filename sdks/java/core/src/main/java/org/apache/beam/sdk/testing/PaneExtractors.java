/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.testing;

import static com.google.common.base.Preconditions.checkState;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.windowing.PaneInfo;
import org.apache.beam.sdk.transforms.windowing.PaneInfo.Timing;
import org.apache.beam.sdk.util.WindowedValue;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PTransform PTransforms} which take an {@link Iterable} of {@link WindowedValue
 * WindowedValues} and outputs an {@link Iterable} of all values in the specified pane, dropping the
 * {@link WindowedValue} metadata.
 *
 * <p>Although all of the method signatures return SimpleFunction, users should ensure to set the
 * coder of any output {@link PCollection}, as appropriate {@link TypeDescriptor TypeDescriptors}
 * cannot be obtained when the extractor is created.
 */
final class PaneExtractors {
  private PaneExtractors() {
  }

  static <T> SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> onlyPane() {
    return new ExtractOnlyPane<>();
  }

  static <T> SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> onTimePane() {
    return new ExtractOnTimePane<>();
  }

  static <T> SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> finalPane() {
    return new ExtractFinalPane<>();
  }

  static <T> SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> nonLatePanes() {
    return new ExtractNonLatePanes<>();
  }

  static <T> SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> allPanes() {
    return new ExtractAllPanes<>();
  }

  private static class ExtractOnlyPane<T>
      extends SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> {
    @Override
    public Iterable<T> apply(Iterable<WindowedValue<T>> input) {
      List<T> outputs = new ArrayList<>();
      for (WindowedValue<T> value : input) {
        checkState(value.getPane().isFirst() && value.getPane().isLast(),
            "Expected elements to be produced by a trigger that fires at most once, but got"
                + "a value in a pane that is %s. Actual Pane Info: %s",
            value.getPane().isFirst() ? "not the last pane" : "not the first pane",
            value.getPane());
        outputs.add(value.getValue());
      }
      return outputs;
    }
  }


  private static class ExtractOnTimePane<T>
      extends SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> {
    @Override
    public Iterable<T> apply(Iterable<WindowedValue<T>> input) {
      List<T> outputs = new ArrayList<>();
      for (WindowedValue<T> value : input) {
        if (value.getPane().getTiming().equals(Timing.ON_TIME)) {
          outputs.add(value.getValue());
        }
      }
      return outputs;
    }
  }


  private static class ExtractFinalPane<T>
      extends SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> {
    @Override
    public Iterable<T> apply(Iterable<WindowedValue<T>> input) {
      List<T> outputs = new ArrayList<>();
      for (WindowedValue<T> value : input) {
        if (value.getPane().isLast()) {
          outputs.add(value.getValue());
        }
      }
      return outputs;
    }
  }


  private static class ExtractAllPanes<T>
      extends SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> {
    @Override
    public Iterable<T> apply(Iterable<WindowedValue<T>> input) {
      List<T> outputs = new ArrayList<>();
      for (WindowedValue<T> value : input) {
        outputs.add(value.getValue());
      }
      return outputs;
    }
  }


  private static class ExtractNonLatePanes<T>
      extends SimpleFunction<Iterable<WindowedValue<T>>, Iterable<T>> {
    @Override
    public Iterable<T> apply(Iterable<WindowedValue<T>> input) {
      List<T> outputs = new ArrayList<>();
      for (WindowedValue<T> value : input) {
        if (value.getPane().getTiming() != PaneInfo.Timing.LATE) {
          outputs.add(value.getValue());
        }
      }
      return outputs;
    }
  }
}
