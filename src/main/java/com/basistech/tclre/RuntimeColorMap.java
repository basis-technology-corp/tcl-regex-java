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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, sharable, color map.
 * The ColorMap data structure is a fully-populated map from all possible char values to shorts,
 * represented in a complex way.
 * This just uses the obvious array of 2^16 shorts. If we wanted to trade space for time,
 * we could use an Short2ShortOpenHashMap instead.
  */
class RuntimeColorMap implements Serializable {
    static final long serialVersionUID = 2L;
    /* for the BMP, we have this array */
    private final transient short[] bmpMap;
    private final RangeMap<Integer, Short> fullMap;

    /**
     * Make a runtime color map. It might be sensible for the BMP optimization
     * to be saved someplace and not recomputed here.
     * @param fullMap -- the map as built in {@link com.basistech.tclre.ColorMap}
     */
    RuntimeColorMap(RangeMap<Integer, Short> fullMap) {
        this.fullMap = fullMap;
        this.bmpMap = new short[Character.MAX_VALUE + 1];
        computeBmp(fullMap);
    }

    private void computeBmp(RangeMap<Integer, Short> fullMap) {
        for (Map.Entry<Range<Integer>, Short> me : fullMap.asMapOfRanges().entrySet()) {
            Range<Integer> range = me.getKey();
            int min = range.lowerEndpoint();
            if (range.lowerBoundType() == BoundType.OPEN) {
                min++;
            }
            if (min < Character.MAX_VALUE) {
                int rmax = range.upperEndpoint();
                if (range.upperBoundType() == BoundType.OPEN) {
                    rmax--;
                }
                int max = Math.min(Character.MAX_VALUE, rmax);
                for (int x = min; x <= max; x++) {
                    this.bmpMap[x] = me.getValue();
                }
            }
        }
    }

    /**
     * Retrieve the color for a character.
     * @param c a character (BMP)
     * @return the color
     */
    short getcolor(char c) {
        return bmpMap[c];
    }

    /**
     * Retrieve the color for a full codepoint.
     * @param codepoint
     * @return
     */
    short getcolor(int codepoint) {
        try {
            return fullMap.get(codepoint);
        } catch (NullPointerException npe) {
            throw new RuntimeException(String.format(" CP %08x no mapping", codepoint));
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        // TreeRangeMap is not Serializable.
        Set<Map.Entry<Range<Integer>, Short>> entries = fullMap.asMapOfRanges().entrySet();
        stream.writeInt(entries.size());
        for (Map.Entry<Range<Integer>, Short> me : entries) {
            stream.writeObject(me.getKey());
            stream.writeShort(me.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            Field dataField = RuntimeColorMap.class.getDeclaredField("bmpMap");
            dataField.setAccessible(true);
            dataField.set(this, new short[Character.MAX_VALUE + 1]);
            dataField = RuntimeColorMap.class.getDeclaredField("fullMap");
            dataField.setAccessible(true);
            dataField.set(this, TreeRangeMap.create());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        int count = in.readInt();
        for (int x = 0; x < count; x ++) {
            Range<Integer> k = (Range<Integer>) in.readObject();
            short v = in.readShort();
            fullMap.put(k, v);
        }
        computeBmp(fullMap);
    }
}
