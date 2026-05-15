# Eri's Kink Stuff!

A Minecraft mod (NeoForge 1.21.1) designed to provide tools for players to express and fulfill various fetishes and roleplay scenarios within the game world. 

## 🔞 Intent & Purpose
This mod is specifically crafted to help players explore kink-related roleplay. It adds functional items and blocks that facilitate power dynamics, sensory play, and pet play themes in a multiplayer environment.

## ✨ Features

### 🐕 Player Leashing System
The core of the mod is a robust, physics-based leashing system that allows players to be tied together.
*   **Collar Requirement:** A player must be wearing a **Collar** (in the Curios necklace slot) to be leashed.
*   **Lead Interaction:** Use a standard vanilla **Lead** on a collared player to leash them.
*   **Auth Physics:** The leash uses server-side physics to pull the leashed player along, including "look-lock" logic that forces the target to face their holder.
*   **Fallback Teleport:** If a leashed player gets stuck behind terrain for too long, they will automatically teleport to their holder.

### ⛓️ The Collar
A Curios item (worn in the necklace slot) that serves as the foundation for various interactions.
*   **Visuals:** Custom 3D model that remains visible on the player's neck.
*   **Dynamic Coloring:** Fully dyeable like leather armor, allowing for deep personalization.
*   **Purpose:** Serves as the required anchor point for the leashing system and the Clicker's obedience logic.

### 🧤 Mittens (Hand Restraints)
A Curios item (worn in the hands slot) that simulates hand restraints.
*   **Restricted Interaction:** While wearing Mittens, a player is physically unable to hold items.
*   **Auto-Clear:** Any item moved into the player's hands will be automatically pushed back into their inventory or dropped if their bags are full.
*   **Visuals:** Custom 3D models for both third-person and first-person views.
*   **Dynamic Coloring:** Can be dyed like leather armour (build up colours by crafting it with dyes)

### 🔔 The Clicker
A training tool for pet play and obedience scenarios.
*   **Look-Lock:** When used, it plays a distinct sound and forces all nearby collared players to immediately snap their gaze toward the user.
*   **Dynamic Coloring:** Can be dyed like leather armour (build up colours by crafting it with dyes)

### 🛏️ Dyeable Pet Beds
A custom 2x2 multi-block structure providing a comfortable place for "pets" to rest.
*   **Dynamic Coloring:** Crafted using Wool and Carpet. The bed inherits a blended color based on the materials used. The bed can also be dyed like leather armour (build up colours by crafting it with dyes)
*   **Functional Bed:** Acts as a respawn point and allowing players to sleep through the night. (Fully compatible with BetterDays)
*   **Visuals:** Custom futon like bed that fits the aesthetic of a pet's corner.

## 🛠️ Requirements
*   **NeoForge** (Version 21.1.228 or higher)
*   **Curios API** (Mandatory for wearing Collars and Mittens)

## 🎨 Crafting
Most items are currently accessible via the custom **"Eri's Kink Stuff"** creative tab. Custom recipes (like the Pet Bed blending) follow standard Minecraft crafting logic but use specialized code for color data.

---
*Developed with love for the kink and minecraft communities.*
