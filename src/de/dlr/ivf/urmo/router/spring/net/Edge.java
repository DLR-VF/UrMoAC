package de.dlr.ivf.urmo.router.spring.net;

public record Edge(long edgeId, long nodeFromId, long nodeToId, double length, double maxSpeed){}
