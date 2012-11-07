/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.wfs.v1_1_0;

import org.geotools.filter.Capabilities;
import org.geotools.filter.capability.FilterCapabilitiesImpl;
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.capability.IdCapabilities;


public class TinyOWSStrategy extends DefaultWFSStrategy {

    public Filter[] splitFilters(Capabilities caps, Filter queryFilter) {
        // ID Filters aren't allowed to be parameters in Logical or Comparison Operators
        
        FilterCapabilities filterCapabilities = caps.getContents();
        IdCapabilities idCapabilities = filterCapabilities.getIdCapabilities();
        if (idCapabilities != null && (idCapabilities.hasEID() || idCapabilities.hasFID())) {
            // server supports ID Filters so we need to check our queryFilter is valid            
            
            Capabilities idFilterCaps = new Capabilities();
            idFilterCaps.addName("Id");
            
            TinyOWSCapabilitiesFilterSplitter splitter = new TinyOWSCapabilitiesFilterSplitter(idFilterCaps, null, null);
            queryFilter.accept(splitter, null);
        
            Filter server = splitter.getFilterPre();
            if (server.equals(Filter.INCLUDE)) {
                // ID Filters not found in the root Filter
                // remove ID Filter from Capabilities
                FilterCapabilities filterCapabilitiesWithoutId = new FilterCapabilitiesImpl(
                        filterCapabilities.getVersion(),
                        filterCapabilities.getScalarCapabilities(),
                        filterCapabilities.getSpatialCapabilities(),
                        null,
                        filterCapabilities.getTemporalCapabilities());

                Capabilities capabilitiesWithoutId = new Capabilities();
                capabilitiesWithoutId.addAll(filterCapabilitiesWithoutId);
                
                return splitFilters(capabilitiesWithoutId, queryFilter);                
            } else {
                // ID Filter found
                // query the server using the ID Filter
                Filter post = splitter.getFilterPost();
                return new Filter[] { server, post };                            
            }
        } else {
            TinyOWSCapabilitiesFilterSplitter splitter = new TinyOWSCapabilitiesFilterSplitter(caps, null, null);

            queryFilter.accept(splitter, null);

            Filter server = splitter.getFilterPre();
            Filter post = splitter.getFilterPost();

            return new Filter[] { server, post };
        }
    }
}