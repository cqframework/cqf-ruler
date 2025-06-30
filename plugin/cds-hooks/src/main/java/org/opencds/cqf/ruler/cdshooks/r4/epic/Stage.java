package org.opencds.cqf.ruler.cdshooks.r4.epic;

enum Stage {
	NOT_STARTED,
	DESERIALISE_REQUEST,
	PREFETCH_RESOLUTION,
	EVALUATE_CQL,
	SERIALISE_RESPONSE,
	COMPLETE
}
