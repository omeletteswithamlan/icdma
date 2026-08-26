package mtu.construction.listener;

import mtu.construction.project.Activity;
import mtu.construction.variable.Rule;

public interface RuleListener
{
	public abstract void ruleTriggered(Rule r, Activity a, Object o);
}
