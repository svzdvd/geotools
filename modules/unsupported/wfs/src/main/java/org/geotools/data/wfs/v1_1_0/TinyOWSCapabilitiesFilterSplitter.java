package org.geotools.data.wfs.v1_1_0;

import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Id;


public class TinyOWSCapabilitiesFilterSplitter extends CapabilitiesFilterSplitter {

    public TinyOWSCapabilitiesFilterSplitter(Capabilities fcs, 
            FeatureType parent, ClientTransactionAccessor transactionAccessor) {
        super(fcs, parent, transactionAccessor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object visit(Id filter, Object notUsed) {
        if (original == null)
            original = filter;
        
        if (!fcs.supports(filter)) {
            postStack.push(filter);
        } else {
            preStack.push(filter);            
        }

        return null;
    }
    
    /*
    @Override
    protected void visitLogicOperator(Filter filter) {
        if (original == null)
            original = filter;

        if (!fcs.supports(filter)) {
            // logical operators aren't supported
            
            // if the logical operator is AND            
            if (filter instanceof And) {
                // test if one of its children is supported
                Iterator<Filter> it = ((And) filter).getChildren().iterator();
                Filter supportedChild = null;
                List<Filter> otherChildren = new ArrayList<Filter>();
                while (it.hasNext()) {
                    Filter child = (Filter) it.next();
                    if (supportedChild == null && fcs.supports(child)) {
                        supportedChild = child;
                    } else {
                        otherChildren.add(child);
                    }
                }
                
                if (supportedChild == null) {
                    postStack.push(filter);
                    return;                                    
                } else {
                    preStack.push(supportedChild);
                    postStack.push(ff.and(otherChildren));
                    return;
                }
            } else {
                postStack.push(filter);
                return;                
            }
        }

        int i = postStack.size();
        int j = preStack.size();
        if (filter instanceof Not) {

            if (((Not) filter).getFilter() != null) {
                Filter next = ((Not) filter).getFilter();
                next.accept(this, null);

                if (i < postStack.size()) {
                    // since and can split filter into both pre and post parts
                    // the parts have to be combined since ~(A^B) == ~A | ~B
                    // combining is easy since filter==combined result however both post and pre
                    // stacks
                    // must be cleared since both may have components of the filter
                    popToSize(postStack, i);
                    popToSize(preStack, j);
                    postStack.push(filter);
                } else {
                    popToSize(preStack, j);
                    preStack.push(filter);
                }
            }
        } else {
            if (filter instanceof Or) {
                Filter orReplacement;

                try {
                    orReplacement = translateOr((Or) filter);
                    orReplacement.accept(this, null);
                } catch (IllegalFilterException e) {
                    popToSize(preStack, j);
                    postStack.push(filter);
                    return;
                }
                if (postStack.size() > i) {
                    popToSize(postStack, i);
                    postStack.push(filter);

                    return;
                }

                preStack.pop();
                preStack.push(filter);
            } else {
                // it's an AND
                Iterator it = ((And) filter).getChildren().iterator();

                while (it.hasNext()) {
                    Filter next = (Filter) it.next();
                    next.accept(this, null);
                }

                // combine the unsupported and add to the top
                if (i < postStack.size()) {
                    if (filter instanceof And) {
                        Filter f = (Filter) postStack.pop();

                        while (postStack.size() > i)
                            f = ff.and(f, (Filter) postStack.pop());

                        postStack.push(f);

                        if (j < preStack.size()) {
                            f = (Filter) preStack.pop();

                            while (preStack.size() > j)
                                f = ff.and(f, (Filter) preStack.pop());
                            preStack.push(f);
                        }
                    } else {
                        LOGGER.warning("LogicFilter found which is not 'and, or, not");

                        popToSize(postStack, i);
                        popToSize(preStack, j);

                        postStack.push(filter);
                    }
                } else {
                    popToSize(preStack, j);
                    preStack.push(filter);
                }
            }
        }
    } */
}