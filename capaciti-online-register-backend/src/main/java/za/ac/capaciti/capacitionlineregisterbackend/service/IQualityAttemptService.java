package za.ac.capaciti.capacitionlineregisterbackend.service;

import za.ac.capaciti.capacitionlineregisterbackend.domain.User;
import za.ac.capaciti.capacitionlineregisterbackend.enums.QualityContext;

public interface IQualityAttemptService {

    int recordFailure(User user, QualityContext context, String contextRef, String feedback);

    void reset(User user, QualityContext context, String contextRef);
}