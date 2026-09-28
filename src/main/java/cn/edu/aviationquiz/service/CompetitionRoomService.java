package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.Session;

/** Authenticated application service exposed to the competition-room controller. */
public interface CompetitionRoomService {
    int submit(Session session, String releaseId, String option);
}
