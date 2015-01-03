/*
 * Copyright 2014 Basis Technology Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.basistech.tclre;

import it.unimi.dsi.fastutil.chars.Char2ShortArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ShortMap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Immutable, sharable, color map.
 * The ColorMap data structure is a fully-populated map from all possible char values to shorts,
 * represented in a complex way.
 * This just uses the obvious array of 2^16 shorts. If we wanted to trade space for time,
 * we could use an Short2ShortOpenHashMap instead.
  */
class RuntimeColorMap implements Serializable {
    static final long serialVersionUID = 1L;
    /* A somewhat sparse representation. */
    private final short[] data;

    /**
     * Construct over a tree. It is the caller's responsibility to make an immutable copy.
     * @param colorMapTree -- the tree as built in the ColorMap.
     */
    RuntimeColorMap(ColorMap.Tree colorMapTree) {
        data = new short[Character.MAX_VALUE + 1];
        for (int x = 0; x < 256; x++) {
            if (colorMapTree.ptrs[x] != null) {
                for (int y = 0; y < 256; y++) {
                    int index = (x * 256) + y;
                    data[index] = colorMapTree.ptrs[x].ccolor[y];
                }
            }
        }
    }

    /**
     * Retrieve the color for a character.
     * @param c
     * @return
     */
    short getcolor(char c) {
        return data[c];
    }

    /*
     * Avoid reading and writing 2^16 shorts by turning it into a sparse data structure.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        Char2ShortMap map = new Char2ShortArrayMap();
        map.defaultReturnValue((short)0);
        for (int x = 0; x <= Character.MAX_VALUE; x++) {
            if (data[x] != 0) {
                map.put((char)x, data[x]);
            }
        }
        out.writeObject(map);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            Field dataField = RuntimeColorMap.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(this, new short[Character.MAX_VALUE + 1]);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        Char2ShortMap map = (Char2ShortMap) in.readObject();
        for (char c : map.keySet()) {
            data[c] = map.get(c); // thank goodness that Java doesn't have actual immutable arrays.
        }
    }
}
