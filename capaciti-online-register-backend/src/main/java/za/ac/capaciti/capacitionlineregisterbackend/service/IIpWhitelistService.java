package za.ac.capaciti.capacitionlineregisterbackend.service;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

public interface IIpWhitelistService {

    String extractClientIp(HttpServletRequest request);

    boolean isWhitelisted(String ip, List<String> cidrs);

    String toCidr(String ip);
}