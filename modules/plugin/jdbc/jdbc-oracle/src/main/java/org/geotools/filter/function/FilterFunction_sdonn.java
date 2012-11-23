/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.filter.function;

import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.filter.expression.VolatileFunction;


/**
 * Oracle function SDO_NN to identify the nearest neighbors for a geometry
 * 
 * @author Davide Savazzi - GeoSolutions
 */
public class FilterFunction_sdonn extends FunctionExpressionImpl implements VolatileFunction {
	
/*	public static FunctionName NAME = new FunctionNameImpl("sdo_nn", String.class,
			FunctionNameImpl.parameter("geometry", Geometry.class), 
			FunctionNameImpl.parameter("sdo_num_res", Integer.class)); */
/*			FunctionNameImpl.parameter("sdo_batch_size", Integer.class, 0, 1)); */
	
    public FilterFunction_sdonn() {
        super("sdo_nn");
    }

	@Override
	public Object evaluate(Object feature) {
		throw new UnsupportedOperationException("Unsupported usage of SDO_NN Oracle function");
	}
} 