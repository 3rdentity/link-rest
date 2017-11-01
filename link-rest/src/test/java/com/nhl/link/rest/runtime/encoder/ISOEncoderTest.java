package com.nhl.link.rest.runtime.encoder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.nhl.link.rest.ResourceEntity;
import com.nhl.link.rest.encoder.Encoder;
import com.nhl.link.rest.encoder.EncoderFilter;
import com.nhl.link.rest.encoder.ISODateEncoder;
import com.nhl.link.rest.encoder.ISODateTimeEncoder;
import com.nhl.link.rest.encoder.ISOTimeEncoder;
import com.nhl.link.rest.encoder.PropertyMetadataEncoder;
import com.nhl.link.rest.it.fixture.cayenne.iso.SqlDateTestEntity;
import com.nhl.link.rest.it.fixture.cayenne.iso.UtilDateTestEntity;
import com.nhl.link.rest.runtime.jackson.JacksonService;
import com.nhl.link.rest.runtime.semantics.RelationshipMapper;
import com.nhl.link.rest.unit.TestWithCayenneMapping;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ISOEncoderTest extends TestWithCayenneMapping {

    // using pre-defined moments in time with and without fractional seconds
    private static final long EPOCH_MILLIS = 1458995247000L;
    private static final long EPOCH_MILLIS_WITH_FRACTION = 1458995247001L;

    private EncoderService encoderService;

    @Before
    public void before() {
        IAttributeEncoderFactory attributeEncoderFactory = new AttributeEncoderFactoryProvider(Collections.emptyMap()).get();
        IStringConverterFactory stringConverterFactory = mock(IStringConverterFactory.class);

        encoderService = new EncoderService(Collections.<EncoderFilter>emptyList(), attributeEncoderFactory, stringConverterFactory,
                new RelationshipMapper(), Collections.<String, PropertyMetadataEncoder>emptyMap());
    }

    /**
     * Since 2.11 we stop distinguishing between different JDBC date types and rely solely on attribute's Java type.
     * Hence, a {@link java.util.Date} attribute is always going to be formatted as ISO 8601 local date-time,
     * regardless of the column's SQL type:
     * {@code yyyy-MM-dd'T'HH:mm:ss[.SSS]}, e.g. 2017-01-01T10:00:00
     *
     * See https://github.com/nhl/link-rest/issues/275
     *
     * @see ISODateTimeEncoder
     */
    @Test
    public void testISODateTimeEncoder_javaUtilDate() {
        _testISODateTimeEncoder_javaUtilDate(new java.util.Date(EPOCH_MILLIS), "yyyy-MM-dd'T'HH:mm:ss");
        _testISODateTimeEncoder_javaUtilDate(new java.util.Date(EPOCH_MILLIS_WITH_FRACTION), "yyyy-MM-dd'T'HH:mm:ss.SSS");
    }

    private void _testISODateTimeEncoder_javaUtilDate(java.util.Date date, String expectedPattern) {

        ResourceEntity<UtilDateTestEntity> resourceEntity = getResourceEntity(UtilDateTestEntity.class);
        appendPersistenceAttribute(resourceEntity, UtilDateTestEntity.DATE, java.util.Date.class, Types.DATE);
        appendPersistenceAttribute(resourceEntity, UtilDateTestEntity.DATE_TIME, java.util.Date.class, 0);
        appendPersistenceAttribute(resourceEntity, UtilDateTestEntity.TIMESTAMP, java.util.Date.class, Types.TIMESTAMP);
        appendPersistenceAttribute(resourceEntity, UtilDateTestEntity.TIME, java.util.Date.class, Types.TIME);

        UtilDateTestEntity utilDateTestEntity = new UtilDateTestEntity();
        utilDateTestEntity.setDate(date);
        utilDateTestEntity.setDateTime(date);
        utilDateTestEntity.setTimestamp(date);
        utilDateTestEntity.setTime(date);

        String dateString = DateTimeFormatter.ofPattern(expectedPattern).format(toLocalDateTime(date));

        assertEquals("{\"data\":[{" +
                "\"date\":\"" + dateString + "\"," +
                "\"dateTime\":\"" + dateString + "\"," +
                "\"time\":\"" + dateString + "\"," +
                "\"timestamp\":\"" + dateString + "\"}]" +
                ",\"total\":1}",
                toJson(utilDateTestEntity, resourceEntity));
    }

    /**
     * {@link java.sql.Date} is always formatted as ISO 8601 local date:
     * {@code yyyy-MM-dd}, e.g. 2017-01-01
     *
     * See https://github.com/nhl/link-rest/issues/275
     *
     * @see ISODateEncoder
     */
    @Test
    public void testISODateEncoder_javaSqlDate() {
        ResourceEntity<SqlDateTestEntity> resourceEntity = getResourceEntity(SqlDateTestEntity.class);
        appendPersistenceAttribute(resourceEntity, SqlDateTestEntity.DATE, java.sql.Date.class, Types.DATE);

        java.sql.Date date = new java.sql.Date(EPOCH_MILLIS);

        SqlDateTestEntity sqlDateTestEntity = new SqlDateTestEntity();
        sqlDateTestEntity.setDate(date);

        String dateString = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .format(toLocalDateTime(date));

        assertEquals("{\"data\":[{\"date\":\"" + dateString + "\"}],\"total\":1}",
                toJson(sqlDateTestEntity, resourceEntity));
    }

    /**
     * {@link java.sql.Time} is always formatted as ISO 8601 local time:
     * {@code HH:mm:ss[.SSS]}, e.g. 10:00:00
     *
     * See https://github.com/nhl/link-rest/issues/275
     *
     * @see ISOTimeEncoder
     */
    @Test
    public void testISOTimeEncoder_javaSqlTime() {
        _testISOTimeEncoder_javaSqlTime(new java.sql.Time(EPOCH_MILLIS), "HH:mm:ss");
        _testISOTimeEncoder_javaSqlTime(new java.sql.Time(EPOCH_MILLIS_WITH_FRACTION), "HH:mm:ss.SSS");
    }

    private void _testISOTimeEncoder_javaSqlTime(java.sql.Time time, String expectedPattern) {

        ResourceEntity<SqlDateTestEntity> resourceEntity = getResourceEntity(SqlDateTestEntity.class);
        appendPersistenceAttribute(resourceEntity, SqlDateTestEntity.TIME, java.sql.Time.class, Types.TIME);

        SqlDateTestEntity sqlDateTestEntity = new SqlDateTestEntity();
        sqlDateTestEntity.setTime(time);

        String timeString = DateTimeFormatter.ofPattern(expectedPattern)
                .format(toLocalDateTime(time));

        assertEquals("{\"data\":[{\"time\":\"" + timeString + "\"}],\"total\":1}", toJson(sqlDateTestEntity, resourceEntity));
    }

    /**
     * {@link java.sql.Timestamp} is always formatted as ISO 8601 local date-time:
     * {@code yyyy-MM-dd'T'HH:mm:ss[.SSS]}, e.g. 2017-01-01T10:00:00
     *
     * See https://github.com/nhl/link-rest/issues/275
     *
     * @see ISODateTimeEncoder
     */
    @Test
    public void testISODateTimeEncoder_javaSqlTimestamp() {
        _testISODateTimeEncoder_javaSqlTimestamp(new java.sql.Timestamp(EPOCH_MILLIS), "yyyy-MM-dd'T'HH:mm:ss");
        _testISODateTimeEncoder_javaSqlTimestamp(new java.sql.Timestamp(EPOCH_MILLIS_WITH_FRACTION), "yyyy-MM-dd'T'HH:mm:ss.SSS");
    }

    private void _testISODateTimeEncoder_javaSqlTimestamp(java.sql.Timestamp timestamp, String expectedPattern) {

        ResourceEntity<SqlDateTestEntity> resourceEntity = getResourceEntity(SqlDateTestEntity.class);
        appendPersistenceAttribute(resourceEntity, SqlDateTestEntity.DATE_TIME, java.sql.Timestamp.class, 0);
        appendPersistenceAttribute(resourceEntity, SqlDateTestEntity.TIMESTAMP, java.sql.Timestamp.class, Types.TIMESTAMP);

        SqlDateTestEntity sqlDateTestEntity = new SqlDateTestEntity();
        sqlDateTestEntity.setDateTime(timestamp);
        sqlDateTestEntity.setTimestamp(timestamp);

        String timestampString = DateTimeFormatter.ofPattern(expectedPattern).format(toLocalDateTime(timestamp));

        assertEquals("{\"data\":[{" +
                "\"dateTime\":\"" + timestampString + "\"," +
                "\"timestamp\":\"" + timestampString + "\"}],\"total\":1}",
                toJson(sqlDateTestEntity, resourceEntity));
    }

    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }

    private String toJson(Object object, ResourceEntity<?> resourceEntity) {

        Encoder encoder = encoderService.dataEncoder(resourceEntity);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            try (JsonGenerator generator = new JacksonService().getJsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
                encoder.encode(null, Collections.singletonList(object), generator);
            }

            return new String(out.toByteArray(), "UTF-8");

        } catch (IOException e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

}