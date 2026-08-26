package mtu.construction.project;

import java.io.Serializable;

/********
 * This skill class refers to the skill of the user of the simulation, not the skill
 * of the laborers in the project.
 * 
 * @author Matt Watkins
 */
public class Skill implements Serializable
{
	private int id;
	private String description;

	/**********************
	 * Gets the description
	 * @return		description
	 */
	public String getDescription() {
		return description;
	}

	/*********************
	 * Sets the description
	 * @param description		new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**********************8
	 * Creates a new skill
	 * @param id				skill id (from the database)
	 * @param description		description of this skill
	 */
	public Skill(int id, String description) {
		super();
		this.id = id;
		this.description = description;
	}

	/********************
	 * Get the ID
	 * @return		id
	 */
	public int getID() {
		return id;
	}

	/**********************
	 * Set the ID
	 * @param id		new id
	 */
	public void setID(int id) {
		this.id = id;
	}

}
