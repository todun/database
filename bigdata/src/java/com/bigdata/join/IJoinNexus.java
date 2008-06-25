/*

Copyright (C) SYSTAP, LLC 2006-2008.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
/*
 * Created on Jun 25, 2008
 */

package com.bigdata.join;

/**
 * Interface provides an interoperability nexus for the {@link IPredicate}s,
 * {@link IBindingSet}s, and {@link ISolution}s for the evaluation of an
 * {@link IRule} and is responsible for resolving the relation symbol to the
 * {@link IRelation} object. Instances of this interface may be type-specific
 * and allow you to control various implementation classes used during
 * {@link IRule} execution.
 * 
 * @todo there is no factory here for an {@link IPredicate}. Do we want one?
 * 
 * @todo perhaps this is the right place to establish the connection between the
 *       relation symbol and functional access to the relation?
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IJoinNexus {

    /**
     * Copy values the values from the visited element corresponding to the
     * given predicate into the binding set.
     * 
     * @param e
     *            An element visited for the <i>predicate</i> using some
     *            {@link IAccessPath}.
     * @param predicate
     *            The {@link IPredicate} providing the {@link IAccessPath}
     *            constraint.
     * @param bindingSet
     *            A set of bindings. The bindings for the predicate will be
     *            copied from the element and set on this {@link IBindingSet} as
     *            a side-effect.
     *            
     * @throws IllegalArgumentException
     *             if any parameter is <code>null</code>.
     */
    void copyValues(Object e, IPredicate predicate, IBindingSet bindingSet);

    /**
     * Create a new element. The element is constructed from the bindings for
     * the head of a rule.
     * 
     * @param predicate
     *            The predicate that is the head of some {@link IRule}.
     * @param bindingSet
     *            A set of bindings for that {@link IRule}.
     * 
     * @return The new element.
     * 
     * @throws IllegalArgumentException
     *             if any parameter is <code>null</code>.
     * @throws IllegalStateException
     *             if the predicate is not fully bound given those bindings.
     */
    Object newElement(IPredicate predicate, IBindingSet bindingSet);

    /**
     * Create a new {@link ISolution}.
     * 
     * @param rule
     *            The rule.
     * @param bindingSet
     *            The bindings (the implementation MUST clone the bindings if
     *            they will be saved with the {@link ISolution}).
     * 
     * @return The new {@link ISolution}.
     * 
     * @throws IllegalArgumentException
     *             if any parameter is <code>null</code>.
     * 
     * @see #isElementOnly()
     */
    ISolution newSolution(IRule rule, IBindingSet bindingSet);

    /**
     * Factory for {@link IBindingSet} implementations.
     * 
     * @param rule
     *            The rule whose bindings will be stored in the binding set.
     * 
     * @return A new binding set suitable for that rule.
     */
    public IBindingSet newBindingSet(IRule rule);
    
    /**
     * Return <code>true</code> if only the element corresponding to the
     * binding on the head of the rule was requested and <code>false</code> if
     * the {@link IRule} and a copy of the {@link IBindingSet} should be
     * included in the {@link ISolution}s generated by the {@link IRule}.
     */
    boolean isElementOnly();
    
    /**
     * Factory for {@link RuleState} instances.
     * 
     * @param rule
     *            The {@link IRule}.
     */
    RuleState newRuleState(IRule rule);

    /**
     * The object that knows how to resolve an {@link IRelationName} to an
     * {@link IRelation}.
     */
    IRelationLocator getRelationLocator();
    
}
