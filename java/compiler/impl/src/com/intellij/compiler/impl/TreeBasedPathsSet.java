/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler.impl;

import com.intellij.util.containers.StringInterner;

/**
 * @author Eugene Zhuravlev
 *         Date: Jun 19, 2006
 */
public class TreeBasedPathsSet {
  private final TreeBasedMap<Object> myMap;

  public TreeBasedPathsSet(StringInterner interner, char separator) {
    myMap = new TreeBasedMap<Object>(interner, separator);
  }

  public void add(String path) {
    myMap.put(path, null);
  }

  public void remove(String path) {
    myMap.remove(path);
  }

  public boolean contains(String path) {
    return myMap.containsKey(path);
  }
}
