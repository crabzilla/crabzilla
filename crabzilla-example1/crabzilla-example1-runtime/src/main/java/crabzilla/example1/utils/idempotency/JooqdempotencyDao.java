package crabzilla.example1.utils.idempotency;

import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static example1.datamodel.tables.Idempotency.IDEMPOTENCY;

public class JooqdempotencyDao {

	Logger logger = LoggerFactory.getLogger(JooqdempotencyDao.class);

	private final String partitionName;
	private final DataSource dataSource;

	public JooqdempotencyDao(String partitionName, DataSource dataSource) {
		this.partitionName = partitionName;
		this.dataSource = dataSource;
	}

	public boolean put(final String slotId) {
		final AtomicBoolean success = new AtomicBoolean(false);
		try {
			DSL.using(dataSource, SQLDialect.MYSQL)
							.transaction(ctx -> {
								Record1<String> record = DSL.using(ctx)
												.select(IDEMPOTENCY.SLOT_ID)
												.from(IDEMPOTENCY)
												.where(IDEMPOTENCY.PARTITION_NAME.eq(partitionName).and(IDEMPOTENCY.SLOT_ID.eq(slotId)))
												.fetchAny();
								if (record!=null) return;
								int _updateCount = DSL.using(ctx)
												.insertInto(IDEMPOTENCY)
												.columns(IDEMPOTENCY.PARTITION_NAME, IDEMPOTENCY.SLOT_ID)
												.values(partitionName, slotId)
												.execute();
								success.set(_updateCount==1);
							});
		} catch (Exception e) {
			//e.printStackTrace();
			return false;
		}
		//logger.info("* put slotId=[{}] success={}", slotId, success.get());
		return success.get();
	}

	public String get(final String key) {
		AtomicReference<String> value = new AtomicReference<>(null);
		DSL.using(dataSource, SQLDialect.MYSQL)
						.transaction(ctx -> {
							Record1<String> record = DSL.using(ctx)
											.select(IDEMPOTENCY.SLOT_ID)
											.from(IDEMPOTENCY)
											.where(IDEMPOTENCY.PARTITION_NAME.eq(partitionName).and(IDEMPOTENCY.SLOT_ID.eq(key)))
											.fetchAny();
							if (record!= null) {
								value.set(record.get(IDEMPOTENCY.SLOT_ID));
							}
						});
		//logger.info("get key={} success={}", key, key.equals(value.get()));
		return value.get();
	}

	public boolean delete(final String key) {
		final AtomicInteger updateCount = new AtomicInteger();
		DSL.using(dataSource, SQLDialect.MYSQL)
						.transaction(ctx -> {
							int _updateCount = DSL.using(ctx)
											.deleteFrom(IDEMPOTENCY)
											.where(IDEMPOTENCY.PARTITION_NAME.eq(partitionName).and(IDEMPOTENCY.SLOT_ID.eq(key)))
											.execute();
							updateCount.set(_updateCount);
						});
		//logger.info("delete key={} success={}", key, updateCount.get()==1);
		return updateCount.get()==1;
	}
}
