/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclre;

/**
* Created by benson on 5/29/14.
*/
class State {
    static final int FREESTATE = -1;
    int no;
    byte flag;		/* marks special states */
    int nins;		/* number of inarcs */
    Arc ins;	/* chain of inarcs */
    int nouts;		/* number of outarcs */
    Arc outs;	/* chain of outarcs */
    Arc free;	/* chain of free arcs */
    State tmp;	/* temporary for traversal algorithms */
    State next;	/* chain for traversing all */
    State prev;	/* back chain */
    ArcBatch oas;	/* first arcbatch, avoid malloc in easy case */
    int noas;		/* number of arcs used in first arcbatch */

    Arc findarc(int type, short co) {
        for (Arc a = outs; a != null; a = a.outchain) {
            if (a.type == type && a.co == co) {
                return a;
            }
        }
        return null;
    }
}
