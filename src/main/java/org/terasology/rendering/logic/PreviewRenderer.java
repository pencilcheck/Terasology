package org.terasology.rendering.logic;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.componentSystem.RenderSystem;
import org.terasology.componentSystem.UpdateSubscriberSystem;
import org.terasology.components.actions.MiniaturizerComponent;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.In;
import org.terasology.entitySystem.RegisterComponentSystem;
import org.terasology.game.CoreRegistry;
import org.terasology.logic.LocalPlayer;
import org.terasology.logic.manager.Config;
import org.terasology.logic.manager.ShaderManager;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.model.structures.BlockCollection;
import org.terasology.model.structures.BlockPosition;
import org.terasology.rendering.AABBRenderer;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.primitives.ChunkMesh;
import org.terasology.rendering.shader.ShaderProgram;
import org.terasology.rendering.world.WorldRenderer;
import org.terasology.world.block.Block;
import org.terasology.world.chunks.Chunk;

import javax.vecmath.Vector3f;

/**
 * Created with IntelliJ IDEA.
 * User: Pencilcheck
 * Date: 1/6/13
 * Time: 2:21 AM
 * To change this template use File | Settings | File Templates.
 */
@RegisterComponentSystem(headedOnly = true)
public class PreviewRenderer implements UpdateSubscriberSystem, RenderSystem {

    @In
    private WorldRenderer worldRenderer;

    @In
    private EntityManager entityManager;

    private BlockCollection collection = null;

    private float rotation = 0f;

    private static final Logger logger = LoggerFactory.getLogger(PreviewRenderer.class);

    @Override
    public void renderOpaque() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renderTransparent() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renderOverlay() {
        //To change body of implemented methods use File | Settings | File Templates.

        //Chunk chunk = worldRenderer.getChunkAtPlayerPosition();
        //renderChunk(chunk);

        for (EntityRef entity : entityManager.iteratorEntities(MiniaturizerComponent.class)) {
            MiniaturizerComponent min = entity.getComponent(MiniaturizerComponent.class);

            if (min.miniatureChunk == null)
                continue;

            if (collection == null) {
                collection = new BlockCollection();
                for (int i=0; i<min.miniatureChunk.getChunkSizeX(); i++) {
                    for (int j=0; j<min.miniatureChunk.getChunkSizeY(); j++) {
                        for (int k=0; k<min.miniatureChunk.getChunkSizeZ(); k++) {
                            Block block = min.miniatureChunk.getBlock(i, j, k);
                            if (block != null && !block.isInvisible() && !block.isPenetrable())
                                collection.addBlock(new BlockPosition(i, j, k), block);
                        }
                    }
                }
            }
            break;
        }

        renderBlockCollection(collection, MiniaturizerComponent.SCALE);
    }

    private void renderBlockCollection(BlockCollection collection, float scale) {
        if (collection != null) {

            ChunkMesh mesh = collection.getMesh();
            if (mesh != null) {
                GL11.glPushMatrix();

                Camera camera = worldRenderer.getActiveCamera();
                LocalPlayer player = CoreRegistry.get(LocalPlayer.class);

                Vector3f dir = new Vector3f(camera.getViewingDirection());
                dir.add(player.getPosition());

                Vector3f cameraPosition = CoreRegistry.get(WorldRenderer.class).getActiveCamera().getPosition();

                GL11.glTranslated(dir.x - cameraPosition.x,
                        dir.y - cameraPosition.y,
                        dir.z - cameraPosition.z);

                GL11.glScaled(1.5f * scale, 1.5f * scale, 1.5f * scale);
                GL11.glRotated(rotation, 0, 1 ,0);

                ShaderManager.getInstance().enableShader("chunk");
                ShaderManager.getInstance().getShaderProgram("chunk").setFloat("blockScale", 1.5f * scale);

                mesh.render(ChunkMesh.RENDER_PHASE.OPAQUE);
                mesh.render(ChunkMesh.RENDER_PHASE.BILLBOARD_AND_TRANSLUCENT);
                mesh.render(ChunkMesh.RENDER_PHASE.WATER_AND_ICE);

                GL11.glPopMatrix();
            }
        }
    }

    private void renderChunk(Chunk chunk) {
        if (chunk != null) {
            //logger.info("renderOverlay");
            if (chunk.getChunkState() == Chunk.State.COMPLETE && chunk.getMesh() != null) {
                GL11.glPushMatrix();

                Camera camera = worldRenderer.getActiveCamera();
                LocalPlayer player = CoreRegistry.get(LocalPlayer.class);

                Vector3f dir = new Vector3f(camera.getViewingDirection());
                dir.add(player.getPosition());

                Vector3f cameraPosition = CoreRegistry.get(WorldRenderer.class).getActiveCamera().getPosition();

                GL11.glTranslated(dir.x - cameraPosition.x,
                        dir.y - cameraPosition.y,
                        dir.z - cameraPosition.z);

                GL11.glScaled(0.01f, 0.01f, 0.01f);
                GL11.glRotated(rotation, 0, 1 ,0);

                /*
                GL11.glTranslated(-chunk.getChunkSizeX()/2,
                        -chunk.getChunkSizeY()/2,
                        -chunk.getChunkSizeZ()/2);
                */

                ShaderManager.getInstance().enableShader("chunk");
                ShaderManager.getInstance().getShaderProgram("chunk").setFloat("blockScale", 0.01f);

                for (int i = 0; i < Chunk.VERTICAL_SEGMENTS; i++) {
                    if (!chunk.getMesh()[i].isEmpty()) {
                        chunk.getMesh()[i].render(ChunkMesh.RENDER_PHASE.OPAQUE);
                        chunk.getMesh()[i].render(ChunkMesh.RENDER_PHASE.BILLBOARD_AND_TRANSLUCENT);
                        chunk.getMesh()[i].render(ChunkMesh.RENDER_PHASE.WATER_AND_ICE);
                    }
                }

                GL11.glPopMatrix();
            }
        }
    }

    @Override
    public void renderFirstPerson() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initialise() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void shutdown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(float delta) {
        //To change body of implemented methods use File | Settings | File Templates.

        rotation += delta * 3.0f;
    }
}
