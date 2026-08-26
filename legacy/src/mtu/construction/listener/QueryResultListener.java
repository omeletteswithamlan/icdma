package mtu.construction.listener;

import mtu.construction.tonae.QueryResult;
import mtu.construction.tonae.QueryResult2;

public interface QueryResultListener {
	public abstract void onQueryFinished(QueryResult2 r);
}
