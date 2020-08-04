package com.mercadolibre.planning_model_me.unit.beans;

import com.mercadolibre.planning_model_me.dtos.SampleDTO;
import com.mercadolibre.planning_model_me.beans.RandomSampleBean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomSampleBeanTest {

	@Test
	public void randomPositiveTestOK() {
		RandomSampleBean randomSample = new RandomSampleBean();

		SampleDTO sample = randomSample.random();
		
		assertTrue(sample.getRandom() >= 0);
	}
}
