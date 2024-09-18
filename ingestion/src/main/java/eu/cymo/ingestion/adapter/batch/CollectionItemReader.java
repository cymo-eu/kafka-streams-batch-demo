package eu.cymo.ingestion.adapter.batch;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.batch.item.ItemReader;

public class CollectionItemReader<T> implements ItemReader<T> {
	private final Iterator<T> iterator;

	public CollectionItemReader(Collection<T> collection) {
		this.iterator = collection.iterator();
	}
	
	@Override
	public T read() throws Exception {
		if(iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

}
