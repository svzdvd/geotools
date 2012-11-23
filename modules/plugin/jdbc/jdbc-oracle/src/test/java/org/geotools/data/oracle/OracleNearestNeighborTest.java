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
package org.geotools.data.oracle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.jdbc.JDBCDataStoreAPITestSetup;
import org.geotools.jdbc.JDBCTestSupport;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.spatial.Equals;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class OracleNearestNeighborTest extends JDBCTestSupport {

	GeometryFactory geomFactory = new GeometryFactory();
	
    @Override
    protected JDBCDataStoreAPITestSetup createTestSetup() {
        return new EmptyJDBCDataStoreAPITestSetup(new OracleTestSetup());
    }

    @Override
    protected void connect() throws Exception {
    	super.connect();
    	
    	// Non-Earth (meters)
    	int srid = 262148;
    	
        deleteSpatialTable("NEIGHBORS");

        setup.run("CREATE TABLE NEIGHBORS (id INT, geometry MDSYS.SDO_GEOMETRY, PRIMARY KEY(id))");
        
        String sql = "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID) " + 
        		"VALUES ('NEIGHBORS','geometry', MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',-180,180,0.5), " + 
        		"MDSYS.SDO_DIM_ELEMENT('Y',-90,90,0.5)), " + srid + ")";
        setup.run(sql);
        
        sql = "CREATE INDEX NEIGHBORS_GEOMETRY_IDX ON NEIGHBORS(GEOMETRY) INDEXTYPE IS MDSYS.SPATIAL_INDEX " +
        		"PARAMETERS ('SDO_INDX_DIMS=2 LAYER_GTYPE=\"POINT\"')";
        setup.run(sql);
        
        int id = 0;
        for (int i = 0; i < 5; i++) {
        	for (int j = 0; j < 5; j++) {
        		setup.run("INSERT INTO NEIGHBORS VALUES (" + (++id) + "," + pointSql(srid, i * 10, j * 10) + ")");  
        	}
        }              
    }

    public void testNearestNeighbor() throws Exception {       
    	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    	ContentFeatureSource source = dataStore.getFeatureSource(tname("NEIGHBORS"));
    	
        SimpleFeatureIterator features = execSdoNN(source, ff, -10, -10, 1, -1, null);
        try {
	        assertTrue(features.hasNext());
	        SimpleFeature f = features.next();
	        Point point = (Point) f.getDefaultGeometry();
	        assertEquals(0.0, point.getCoordinate().x);
	        assertEquals(0.0, point.getCoordinate().y);        
	        assertFalse(features.hasNext());
        } finally {
        	features.close();
        }
        
        features = execSdoNN(source, ff, 100, 100, 1, -1, null);
        try {
	        assertTrue(features.hasNext());
	        SimpleFeature f = features.next();
	        Point point = (Point) f.getDefaultGeometry();
	        assertEquals(40.0, point.getCoordinate().x);
	        assertEquals(40.0, point.getCoordinate().y);        
	        assertFalse(features.hasNext());
        } finally {
        	features.close();
        }        
        
        features = execSdoNN(source, ff, -10, -10, 3, -1, null);
        try {
        	assertEquals(3, getSize(features));
        } finally {
        	features.close();
        }
        
        // test using sdo_batch_size hint
        features = execSdoNN(source, ff, -10, -10, 3, 10, "INCLUDE");
        try {
        	assertEquals(3, getSize(features));
        } finally {
        	features.close();
        }   
        
        // test using CQL and no batch_size
        features = execSdoNN(source, ff, -10, -10, 3, -1, "INCLUDE");
        try {
        	assertEquals(3, getSize(features));
        } finally {
        	features.close();
        }         
        
        // test with limit greater than rows
        features = execSdoNN(source, ff, -10, -10, 50, -1, null);
        try {
        	assertEquals(25, getSize(features));
        } finally {
        	features.close();
        }        
    }
    
    private int getSize(SimpleFeatureIterator features) {
    	int counter = 0;
    	while (features.hasNext()) {
    		features.next();
    		counter++;
    	}
    	return counter;
    }
    
    private SimpleFeatureIterator execSdoNN(ContentFeatureSource source, FilterFactory2 ff, double x, double y, int limit, int batch, String cql) throws IOException {
    	List<Expression> params = new ArrayList<Expression>();
    	params.add(ff.literal(point(x, y)));
    	params.add(ff.literal(limit));

    	if (cql != null) {
    		params.add(ff.literal(cql));
    		
        	if (batch > 0) {
        		params.add(ff.literal(batch));
        	}
    	}

    	Function sdo_nn = ff.function("sdo_nn", params.toArray(new Expression[params.size()]));    	
    	Equals equalsFilter = ff.equal(sdo_nn, ff.literal(true));
        Query query = new Query(tname("NEIGHBORS"), equalsFilter);
        
        SimpleFeatureCollection features = source.getFeatures(query);
        return features.features();
    }
    
    private Point point(double x, double y) {
    	return geomFactory.createPoint(new Coordinate(x, y));
    }

    private void deleteSpatialTable(String name) throws Exception {
    	try {
    		setup.run("DROP TABLE " + name + " purge");
    	} catch (Exception e) {
    	}
    	
    	setup.run("DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = '" + name + "'");
    }
    
    private String pointSql(int srid, double x, double y) {
    	return "MDSYS.SDO_GEOMETRY(2001," + srid + ",SDO_POINT_TYPE(" + x + "," + y + ",NULL),NULL,NULL)";
    }
}
