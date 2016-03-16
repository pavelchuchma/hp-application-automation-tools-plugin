package com.hp.nga.integrations.dto.general;

import com.hp.nga.integrations.dto.DTOBase;

/**
 * Created by gullery on 06/01/2016.
 * <p>
 * CI Jobs list container descriptor
 */

public interface CIJobsList extends DTOBase {

	void setJobs(CIJobMetadata[] jobs);

	CIJobMetadata[] getJobs();
}
