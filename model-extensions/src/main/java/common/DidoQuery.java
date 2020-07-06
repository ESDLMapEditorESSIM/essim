package common;

import org.influxdb.dto.Query;

public class DidoQuery extends Query {

	public DidoQuery(String command, String database) {
		super(command, database);
	}

	@Override
	public String getCommandWithUrlEncoded() {
		return super.getCommandWithUrlEncoded().replace("+", "%20");
	}
}
