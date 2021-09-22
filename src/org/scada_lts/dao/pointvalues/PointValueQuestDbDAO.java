/*
 * (c) 2015 Abil'I.T. http://abilit.eu/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.scada_lts.dao.pointvalues;

import com.serotonin.mango.DataTypes;
import com.serotonin.mango.rt.dataImage.AnnotatedPointValueTime;
import com.serotonin.mango.rt.dataImage.PointValueTime;
import com.serotonin.mango.rt.dataImage.types.*;
import com.serotonin.mango.vo.DataPointVO;
import com.serotonin.mango.vo.bean.LongPair;
import com.serotonin.mango.vo.bean.PointHistoryCount;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.scada_lts.dao.DAO;
import org.scada_lts.dao.DataPointDAO;
import org.scada_lts.dao.model.point.PointValue;
import org.scada_lts.dao.model.point.PointValueAdnnotation;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


@Repository
public class PointValueQuestDbDAO implements IPointValueQuestDbDAO {

    private static final Log LOG = LogFactory.getLog(PointValueQuestDbDAO.class);

    private static IPointValueQuestDbDAO instance;

    private final static String  COLUMN_NAME_DATA_TYPE = "dataType";
    private final static String  COLUMN_NAME_POINT_VALUE = "pointValue";
    private final static String  COLUMN_NAME_TEXT_POINT_VALUE_SHORT = "textPointValueShort";
    private final static String  COLUMN_NAME_TEXT_POINT_VALUE_LONG = "textPointValueLong";
    private final static String  COLUMN_NAME_SOURCE_TYPE = "sourceType";
    private final static String  COLUMN_NAME_TS = "ts";
    private final static String  COLUMN_NAME_TIMESTAMP = "timestamp";
    private final static String  COLUMN_NAME_SOURCE_ID = "sourceId";
    private final static String  COLUMN_NAME_DATA_POINT_ID = "dataPointId";
    private final static String  COLUMN_NAME_MIN_TIME_STAMP = "minTs";
    private final static String  COLUMN_NAME_MAX_TIME_STAMP = "maxTs";
    private final static String  COLUMN_NAME_USERNAME_IN_TABLE_USERS = "username";

    private final static String TABLE_POINT_VALUES = "pointValues$dpId";


    // @formatter:off
    private static final String POINT_VALUE_SELECT = ""
            + "select "
            + COLUMN_NAME_DATA_TYPE + ", "
            + COLUMN_NAME_POINT_VALUE + ", "
            + COLUMN_NAME_TS + ", "
            + COLUMN_NAME_TEXT_POINT_VALUE_SHORT + ", "
            + COLUMN_NAME_TEXT_POINT_VALUE_LONG + ", "
            + COLUMN_NAME_SOURCE_TYPE + ", "
            + COLUMN_NAME_SOURCE_ID + ", "
            + COLUMN_NAME_USERNAME_IN_TABLE_USERS + " "
            + "from "
            + TABLE_POINT_VALUES + " ";

    private static final String UNION_ALL = " union all ";

    private static final String POINT_VALUE_SELECT_MIN_MAX_TS_BASE_ON_LIST_DATA_POINT = ""
            +"select "
            +"min(ts) as "+COLUMN_NAME_MIN_TIME_STAMP+", "
            +"max(ts) as "+COLUMN_NAME_MAX_TIME_STAMP+" "
            +"from "
            + TABLE_POINT_VALUES + " ";

    private static final String POINT_VALUE_SELECT_MIN_TS_BASE_ON_LIST_DATA_POINT = ""
            +"select "
            +"min(ts) as "+ COLUMN_NAME_MIN_TIME_STAMP + " "
            +"from "
            + TABLE_POINT_VALUES + " ";

    private static final String POINT_VALUE_SELECT_MAX_TS_BASE_ON_LIST_DATA_POINT = ""
            +"select "
            + "max(ts) as "+ COLUMN_NAME_MAX_TIME_STAMP+" "
            +"from "
            + TABLE_POINT_VALUES + " ";

    private static final String POINT_VALUE_SELECT_MAX_BASE_ON_DATA_POINT_ID = ""
            +"select "
            +"max(ts) as " + COLUMN_NAME_MAX_TIME_STAMP + " "
            +"from "
            + TABLE_POINT_VALUES + " ";

    private static final String POINT_VALUE_SELECT_ON_BASE_ID_TS = ""
            + POINT_VALUE_SELECT
            + " where "
            + COLUMN_NAME_TS +"=?";

    private static final String POINT_VALUE_INSERT = ""
            + "insert into "
            + TABLE_POINT_VALUES + " ("
            + ""+COLUMN_NAME_DATA_TYPE + ", "
            + ""+COLUMN_NAME_POINT_VALUE + ", "
            + ""+COLUMN_NAME_TS + ", "
            + ""+COLUMN_NAME_TIMESTAMP + ", "
            + ""+COLUMN_NAME_TEXT_POINT_VALUE_SHORT + ", "
            + ""+COLUMN_NAME_TEXT_POINT_VALUE_LONG + ", "
            + ""+COLUMN_NAME_SOURCE_TYPE + ", "
            + ""+COLUMN_NAME_SOURCE_ID + ", "
            + ""+COLUMN_NAME_USERNAME_IN_TABLE_USERS
            +") "
            + "values (?,?,?,to_timezone(?, 'CET'),?,?,?,?,?)";

    private static final String POINT_VALUE_INCEPTION_DATA = " "
            +"select "
            + "min(ts) "
            + "from "
            + TABLE_POINT_VALUES + " ";

    private static final String POINT_VALUE_DATA_RANGE_COUNT = " "
            +"select "
            + "count(*) "
            + "from "
            + TABLE_POINT_VALUES + " "
            + "where "
            + COLUMN_NAME_TS +">=? and "+ COLUMN_NAME_TS +"<=?";

    private static final String SELECT_MAX_TIME_WHERE_DATA_POINT_ID = ""
            + "select max("
            + COLUMN_NAME_TS + ") "
            + TABLE_POINT_VALUES + " ";

    private static final String SELECT_MIN_TIME_WHERE_DATA_POINT_ID = ""
            + "select min("
            + COLUMN_NAME_TS + ") "
            + TABLE_POINT_VALUES + " ";

    private static final String DROP_PARTITION = ""
            + "alter table "
            + TABLE_POINT_VALUES
            + " drop partition";

    private static final String WHERE_TIMESTAMP = " WHERE " + COLUMN_NAME_TIMESTAMP + " < ";

    private static final String TO_TIMEZONE = ""
            + "to_timezone($timestamp, 'CET')";

    public static final String POINT_VALUE_FILTER_BASE_ON_DATA_POINT_ID_AND_TIME_STAMP = " "
            + "pv."+COLUMN_NAME_TIMESTAMP+" >= to_timezone($from, 'CET') order by "+COLUMN_NAME_TIMESTAMP;

    public static final String POINT_VALUE_FILTER_BASE_ON_DATA_POINT_ID_AND_TIME_STAMP_FROM_TO = " "
            + "pv."+COLUMN_NAME_TIMESTAMP+">=to_timezone($from, 'CET') and pv."+COLUMN_NAME_TIMESTAMP+"<to_timezone($to, 'CET') order by "+COLUMN_NAME_TIMESTAMP;

    public static final String POINT_VALUE_FILTER_LAST_BASE_ON_DATA_POINT_ID = " "
            + "order by pv."+COLUMN_NAME_TIMESTAMP+" desc";

    public static final String POINT_VALUE_FILTER_LATEST_BASE_ON_DATA_POINT_ID = " "
            + "pv."+COLUMN_NAME_TIMESTAMP+"<? "
            + "order by pv."+COLUMN_NAME_TIMESTAMP+" desc";

    public static final String POINT_VALUE_FILTER_BEFORE_TIME_STAMP_BASE_ON_DATA_POINT_ID = " "
            + "pv."+COLUMN_NAME_TIMESTAMP+"<to_timezone($to, 'CET') "
            + "order by pv."+COLUMN_NAME_TIMESTAMP;

    public static final String POINT_VALUE_FILTER_AT_TIME_STAMP_BASE_ON_DATA_POINT_ID = " "
            + "pv."+COLUMN_NAME_TIMESTAMP+"=to_timezone($time, 'CET') "
            + "order by pv."+COLUMN_NAME_TIMESTAMP;

    public static final String CREATE_TABLE_FOR_DATAPOINT = " "
            + "create table if not exists "
            + TABLE_POINT_VALUES + " "
            + "(dataType INT, pointValue DOUBLE, ts LONG, timestamp TIMESTAMP,\n"
            + "textPointValueShort SYMBOL, textPointValueLong SYMBOL, sourceType INT, sourceId INT, username SYMBOL) \n"
            + "timestamp(timestamp) partition by DAY";

    private static final String DATAPOINT_ID = "$dpId";

    // @formatter:on

    //RowMappers
    private class PointValueRowMapper implements RowMapper<PointValue> {
        private long dataPointId;

        public PointValueRowMapper(long dataPointId) {
            this.dataPointId = dataPointId;
        }

        public PointValue mapRow(ResultSet rs, int rowNum) throws SQLException {
            //TODO rewrite MangoValue
            MangoValue value = createMangoValue(rs);
            long time = rs.getLong(COLUMN_NAME_TS);

            PointValue pv = new PointValue();
            //pv.setId(rs.getLong(COLUMN_NAME_ID));
            pv.setDataPointId(this.dataPointId);
            int sourceId = rs.getInt(COLUMN_NAME_SOURCE_ID);

            int sourceType = rs.getInt(COLUMN_NAME_SOURCE_TYPE);

            if (rs.wasNull()) {
                pv.setPointValue(new PointValueTime(value, time));
            } else {
                pv.setPointValue(new AnnotatedPointValueTime(value, time, sourceType, sourceId));
            }
            return pv;
        }
    }

    private class PointValueRowMapperWithUserName implements RowMapper<PointValue> {

        private long dataPointId;

        public PointValueRowMapperWithUserName(long dataPointId) {
            this.dataPointId = dataPointId;
        }

        public PointValue mapRow(ResultSet rs, int rowNum) throws SQLException {
            //TODO rewrite MangoValue
            MangoValue value = createMangoValue(rs);
            long time = rs.getLong(COLUMN_NAME_TS);

            PointValue pv = new PointValue();
            //pv.setId(rs.getLong(COLUMN_NAME_ID));
            pv.setDataPointId(this.dataPointId);
            int sourceId = rs.getInt(COLUMN_NAME_SOURCE_ID);

            int sourceType = rs.getInt(COLUMN_NAME_SOURCE_TYPE);
            int username = -1;
            String userName = null;
            try{
                username =  rs.getInt(COLUMN_NAME_USERNAME_IN_TABLE_USERS);
            }
            catch (Exception e){

                userName = rs.getString(COLUMN_NAME_USERNAME_IN_TABLE_USERS);
            }

            pv.setPointValue( rs.wasNull()
                    ? createPointValueTime( userName, value, time, username )
                    : createAnnotatedPointValueTime( value,time, sourceType, sourceId, userName ));

            return pv;
        }
        private AnnotatedPointValueTime createAnnotatedPointValueTime(MangoValue value, long time, int sourceType, int sourceId, String userName){
            return ( userName!= null )
                    ?new AnnotatedPointValueTime(value, time, sourceType, sourceId, userName)
                    :new AnnotatedPointValueTime(value, time, sourceType, sourceId);
        }
        private PointValueTime createPointValueTime(String userName, MangoValue value, long time, int username ){

            if(userName!= null){
                return new PointValueTime(value, time,userName);
            }
            else if( username!=0 ){
                return new PointValueTime(value, time,String.valueOf(username));
            }
            else {
                return new PointValueTime(value, time);
            }
        }
    }

    private class LongPairRowMapper implements RowMapper<LongPair> {
        public LongPair mapRow(ResultSet rs, int index) throws SQLException {
            long myLongValue = rs.getLong(COLUMN_NAME_MIN_TIME_STAMP);
            if (rs.wasNull())
                return null;
            return new LongPair(myLongValue, rs.getLong(COLUMN_NAME_MAX_TIME_STAMP));
        }
    }

    private class LongRowMapper implements RowMapper<Long> {
        public Long mapRow(ResultSet rs, int index) throws SQLException {
            return rs.getLong(COLUMN_NAME_MIN_TIME_STAMP);
        }
    }

    //TODO rewrite for new types
    MangoValue createMangoValue(ResultSet rs)
            throws SQLException {

        int dataType = rs.getInt(COLUMN_NAME_DATA_TYPE);
        MangoValue value = null;
        switch (dataType) {
            case (DataTypes.NUMERIC):
                value = new NumericValue(rs.getDouble(COLUMN_NAME_POINT_VALUE));
                break;
            case (DataTypes.BINARY):
                value = new BinaryValue(rs.getDouble(COLUMN_NAME_POINT_VALUE)==1);
                break;
            case (DataTypes.MULTISTATE):
                value = new MultistateValue(rs.getInt(COLUMN_NAME_POINT_VALUE));
                break;
            case (DataTypes.ALPHANUMERIC):
                String s = rs.getString(COLUMN_NAME_TEXT_POINT_VALUE_SHORT);
                if (s == null)
                    s = rs.getString(COLUMN_NAME_TEXT_POINT_VALUE_LONG);
                value = new AlphanumericValue(s);
                break;
            case (DataTypes.IMAGE): {
                try {
                    value = new ImageValue(Integer.parseInt(rs
                            .getString(COLUMN_NAME_TEXT_POINT_VALUE_SHORT)),
                            rs.getInt(COLUMN_NAME_TEXT_POINT_VALUE_LONG));
                } catch (NumberFormatException e) {
                    LOG.info(e.getMessage());
                }
                break;
            }
            default:
                value = null;
        }
        return value;
    }

    public static IPointValueQuestDbDAO getInstance() {
        if (instance == null) {
            instance = new PointValueQuestDbDAO(DAO.query().getJdbcTemp());
        }
        return instance;
    }

    private JdbcOperations jdbcTemplate;

    public PointValueQuestDbDAO() {
        this.jdbcTemplate = DAO.getInstance().getJdbcTemp();
    }

    public PointValueQuestDbDAO(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
/*
	@Override
	public PointValue findById(Object[] pk) {
		return (PointValue) jdbcTemplate.queryForObject(POINT_VALUE_SELECT_ON_BASE_ID, pk, new PointValueRowMapper());
	}*/

    public List<PointValue> findByIdAndTs(long id, long ts) {
        String query = POINT_VALUE_SELECT_ON_BASE_ID_TS.replace(DATAPOINT_ID, String.valueOf(id));
        return jdbcTemplate.query(query, new Object[] { ts }, new PointValueRowMapper(id));
    }

    @Override
    public List<PointValue> filtered(String filter, Object[] argsFilter, long limit) {
        long dpId = ((Number) argsFilter[0]).longValue();
        String myLimit="";
        if (limit != NO_LIMIT) {
            //TODO rewrite limit adding in argsFilter
            myLimit = LIMIT+limit;
        }
        List<PointValue> res =
                (List<PointValue>) jdbcTemplate.query(
                        "select "
                                + "pv."+COLUMN_NAME_DATA_TYPE + ", "
                                + "pv."+COLUMN_NAME_POINT_VALUE + ", "
                                + "pv."+ COLUMN_NAME_TS + ", "
                                + "pv."+COLUMN_NAME_TEXT_POINT_VALUE_SHORT + ", "
                                + "pv."+COLUMN_NAME_TEXT_POINT_VALUE_LONG + ", "
                                + "pv."+COLUMN_NAME_SOURCE_TYPE + ", "
                                + "pv."+COLUMN_NAME_SOURCE_ID + ", "
                                + "pv." +COLUMN_NAME_USERNAME_IN_TABLE_USERS
                                + " from "
                                + "pointValues" + dpId + " pv where "+ filter + myLimit, new PointValueRowMapperWithUserName(dpId));
        return res;
    }

    @Transactional(readOnly = false,propagation= Propagation.REQUIRES_NEW,isolation= Isolation.READ_COMMITTED,rollbackFor=SQLException.class)
    @Override
    public Object[] create(final PointValue entity) {

        if (LOG.isTraceEnabled()) {
            LOG.trace(entity);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                String query = POINT_VALUE_INSERT.replace(DATAPOINT_ID, String.valueOf(entity.getDataPointId()));
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                new ArgumentPreparedStatementSetter( new Object[] {
                        entity.getPointValue().getValue().getDataType(),
                        getValueBaseOnType(entity.getPointValue().getValue().getDataType(), entity.getPointValue()),
                        entity.getPointValue().getTime(),
                        entity.getPointValue().getTime()*1000,
                        null,
                        null,
                        null,
                        null,
                        null
                }).setValues(ps);
                return ps;
            }
        }, keyHolder);

        return new Object[] {keyHolder.getKey().longValue()};

    }


    @Transactional(readOnly = false,propagation= Propagation.REQUIRES_NEW,isolation= Isolation.READ_COMMITTED,rollbackFor=SQLException.class)
    public Object[] create(final int pointId,final int dataType,final double dvalue,final long time) {

        return createNoTransaction(pointId, dataType, dvalue, time);

    }

    @Override
    public void create(PointValue pointValue, PointValueAdnnotation pointValueAdnnotation) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("pointId:"+pointValue.getDataPointId()+" dataType:"+pointValue.getPointValue()
                    .getValue().getDataType()+" time:"+pointValue.getPointValue().getTime());
        }

        PointValueTime pointValueTime = pointValue.getPointValue();
        MangoValue mangoValue = pointValueTime.getValue();

        jdbcTemplate.update(connection -> {
            String query = POINT_VALUE_INSERT.replace(DATAPOINT_ID, String.valueOf(pointValue.getDataPointId()));
            PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            new ArgumentPreparedStatementSetter( new Object[] {
                    mangoValue.getDataType(),
                    getValueBaseOnType(mangoValue.getDataType(), pointValueTime),
                    pointValueTime.getTime(),
                    pointValueTime.getTime()*1000,
                    pointValueAdnnotation.getTextPointValueShort(),
                    pointValueAdnnotation.getTextPointValueLong(),
                    pointValueAdnnotation.getSourceType(),
                    pointValueAdnnotation.getSourceId(),
                    pointValueAdnnotation.getChangeOwner()
            }).setValues(ps);
            return ps;
        });
    }


    public Object[] createNoTransaction(final int pointId,final int dataType,final double dvalue,final long time) {

        if (LOG.isTraceEnabled()) {
            LOG.trace("pointId:"+pointId+" dataType:"+dataType+" dvalue:"+time);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                String query = POINT_VALUE_INSERT.replace(DATAPOINT_ID, String.valueOf(pointId));
                PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                new ArgumentPreparedStatementSetter( new Object[] {
                        dataType,
                        dvalue,
                        time,
                        time*1000,
                        null,
                        null,
                        null,
                        null,
                        null
                }).setValues(ps);
                return ps;
            }
        }, keyHolder);

        try {
            return new Object[]{keyHolder.getKey().longValue()};
        } catch (InvalidDataAccessApiUsageException ex) {
            return new Object[]{Long.parseLong(String.valueOf(keyHolder.getKeys().get("id")))};
        }

    }

    @Transactional(readOnly = false,propagation= Propagation.REQUIRES_NEW,isolation= Isolation.READ_COMMITTED,rollbackFor=SQLException.class)
    public void executeBatchUpdateInsert( List<Object[]> params) {

        Map<Object, List<Object[]>> groupedParams = params.stream().collect(Collectors.groupingBy(p -> p[0]));

        if (LOG.isTraceEnabled()) {
            for (Object[] param : params) {
                for (Object arg :param) {
                    LOG.trace("arg:"+arg);
                }
            }
        }
        if(params.isEmpty()) {
            return;
        }
        groupedParams.forEach((key, value) -> {
            String query = POINT_VALUE_INSERT.replace(DATAPOINT_ID, String.valueOf(key));
            if(value.get(0).length == 4) {
                List<Object[]> paramsExt = new ArrayList<>();
                for (Object[] args : value) {
                    paramsExt.add(new Object[]{args[1], args[2], args[3], (long)args[3]*1000, null, null, null, null, null});
                }
                jdbcTemplate.batchUpdate(query, paramsExt);
            } else
                jdbcTemplate.batchUpdate(query, value);
        });
    }

    public Long getInceptionDate(int dataPointId) {
        String query = POINT_VALUE_INCEPTION_DATA.replace(DATAPOINT_ID,  String.valueOf(dataPointId));
        return jdbcTemplate.queryForObject(query, Long.class);
    }

    public long dateRangeCount(int dataPointId, long from, long to) {
        String query = POINT_VALUE_DATA_RANGE_COUNT.replace(DATAPOINT_ID,  String.valueOf(dataPointId));
        return jdbcTemplate.queryForObject(query, new Object[] {from, to}, Long.class);
    }

    public LongPair getStartAndEndTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty())	return null;

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids",dataPointIds);

        String query = POINT_VALUE_SELECT_MIN_MAX_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(0)));;

        for (int i = 1; i < dataPointIds.size(); i++) {
            query += UNION_ALL + POINT_VALUE_SELECT_MIN_MAX_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(i)));;
        }

        LongPair longPair = jdbcTemplate.queryForObject(query,new LongPairRowMapper(),parameters);

        return longPair;
    }

    public long getStartTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty())	return -1;

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids",dataPointIds);

        String query = POINT_VALUE_SELECT_MIN_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(0)));;

        for (int i = 1; i < dataPointIds.size(); i++) {
            query += UNION_ALL + POINT_VALUE_SELECT_MIN_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(i)));;
        }

        Long minTs = jdbcTemplate.queryForObject(query,new LongRowMapper(),parameters);

        return minTs;
    }

    public long getEndTime(List<Integer> dataPointIds) {
        if (dataPointIds.isEmpty()) return -1;
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids",dataPointIds);

        String query = POINT_VALUE_SELECT_MAX_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(0)));

        for (int i = 1; i < dataPointIds.size(); i++) {
            query += UNION_ALL + POINT_VALUE_SELECT_MAX_TS_BASE_ON_LIST_DATA_POINT.replace(DATAPOINT_ID, String.valueOf(dataPointIds.get(i)));
        }

        Long maxTs = jdbcTemplate.queryForObject(query, new LongRowMapper(),parameters);

        return maxTs;

    }

    public Long getLatestPointValue(int dataPointId) {
        try {
            String query = POINT_VALUE_SELECT_MAX_BASE_ON_DATA_POINT_ID.replace(DATAPOINT_ID, String.valueOf(dataPointId));
            return  jdbcTemplate.queryForObject(query, Long.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public PointValue getPointValueRow(ResultSet rs, int rowNum) {
        return null;
    }

    /**
     * When save value ALPHANUMERIC or IMAGE then data save in adnnotations
     * @param dataType
     * @param value
     * @return
     */
    private double getValueBaseOnType(int dataType, PointValueTime value) {
        Double avalue = null;
        if ( (dataType==DataTypes.ALPHANUMERIC) || (dataType==DataTypes.IMAGE) || value == null) {
            avalue = 0.0;
        } else {
            avalue = value.getDoubleValue();
        }
        return avalue;
    }

    // Apply database specific bounds on double values.
    public double applyBounds(double value) {
        if (Double.isNaN(value))
            return 0;
        if (value == Double.POSITIVE_INFINITY)
            return Double.MAX_VALUE;
        if (value == Double.NEGATIVE_INFINITY)
            return -Double.MAX_VALUE;

        return value;
    }

    public long deletePointValuesBeforeWithOutLast(int dataPointId, long time) {
        String ts = TO_TIMEZONE.replace("$timestamp", String.valueOf(time*1000));
        String query = DROP_PARTITION.replace(DATAPOINT_ID, String.valueOf(dataPointId));
        return jdbcTemplate.update(query + WHERE_TIMESTAMP + ts);
    }

    public long deletePointValue(int dataPointId) {
        String query = DROP_PARTITION.replace(DATAPOINT_ID, String.valueOf(dataPointId));
        return jdbcTemplate.update(query);
    }

    public long deleteAllPointData() {
        List<DataPointVO> dataPoints = new DataPointDAO().getDataPoints();
        for (DataPointVO dataPoint : dataPoints) {
            deletePointValue(dataPoint.getId());
        }
        return 0;
    }

    public long getMinTs(int dataPointId) {
        String query = SELECT_MIN_TIME_WHERE_DATA_POINT_ID.replace(DATAPOINT_ID, String.valueOf(dataPointId));
        return jdbcTemplate.queryForObject(query, Long.TYPE);
    }

    public long getMaxTs(int dataPointId) {
        String query = SELECT_MAX_TIME_WHERE_DATA_POINT_ID.replace(DATAPOINT_ID, String.valueOf(dataPointId));
        return jdbcTemplate.queryForObject(query, Long.TYPE);
    }

    public void createTableForDatapoint(int dpId) {
        String query = CREATE_TABLE_FOR_DATAPOINT.replace(DATAPOINT_ID, String.valueOf(dpId));
        jdbcTemplate.update(query);
    }

    @Override
    public List<Long> getFiledataIds() {
        return null;
    }

    @Override
    public List<PointHistoryCount> getTopPointHistoryCounts() {
        return null;
    }

    @Override
    public List<PointValue> findAll() {
        return null;
    }

    @Override
    public List<PointValue> findAllWithUserName() {
        return null;
    }

    @Override
    public long deletePointValuesWithMismatchedType(int dataPointId, int dataType) {
        return 0;
    }

}